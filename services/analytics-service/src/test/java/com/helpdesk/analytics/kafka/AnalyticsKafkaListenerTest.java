package com.helpdesk.analytics.kafka;

import tools.jackson.databind.ObjectMapper;
import com.helpdesk.analytics.domain.TicketMetric;
import com.helpdesk.analytics.domain.TicketMetricRepository;
import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * handleEvent() is the only real logic in this service - it decides how a raw Kafka payload
 * turns into a TicketMetric row. There's no Spring context or actual Kafka broker involved
 * here on purpose: the @KafkaListener annotation just routes messages to this method, so
 * calling it directly proves the same branching a real message would trigger, without the
 * cost or flakiness of spinning up a broker.
 */
class AnalyticsKafkaListenerTest {

    private final ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();

    @Test
    void ticketCreatedEventCreatesNewMetricWithCategoryAndOpenStatus() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.findByTicketId(ticketId)).thenReturn(null);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        String payload = """
                {"eventType": "ticket.created", "ticketId": "%s", "category": "BUG"}
                """.formatted(ticketId);
        listener.handleEvent(payload);

        org.mockito.ArgumentCaptor<TicketMetric> captor = org.mockito.ArgumentCaptor.forClass(TicketMetric.class);
        verify(repository).save(captor.capture());
        TicketMetric saved = captor.getValue();
        assertThat(saved.getTicketId()).isEqualTo(ticketId);
        assertThat(saved.getCategory()).isEqualTo("BUG");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.isSlaBreached()).isFalse();
    }

    @Test
    void handlesARealTicketCreatedEventAsActuallyPublishedByTicketService() throws Exception {
        // The other tests above hand-write JSON with "eventType" already in it, which would
        // have stayed green even while eventType() was silently missing from the real
        // TicketController -> OutboxWorker -> Kafka payload (a real bug this test would have
        // caught immediately). This one goes through the same ObjectMapper.writeValueAsString
        // call the producer actually uses, so it fails the moment that wire format regresses.
        ObjectMapper realObjectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        UUID ticketId = UUID.randomUUID();
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(),
                TicketCreatedEvent.CURRENT_VERSION, Instant.now(), ticketId,
                TicketCategory.BUG, "requester-1");
        String payload = realObjectMapper.writeValueAsString(event);

        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        when(repository.findByTicketId(ticketId)).thenReturn(null);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, realObjectMapper);

        listener.handleEvent(payload);

        org.mockito.ArgumentCaptor<TicketMetric> captor = org.mockito.ArgumentCaptor.forClass(TicketMetric.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("BUG");
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
    }

    @Test
    void slaBreachedEventUpdatesExistingMetricInPlace() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        TicketMetric existing = new TicketMetric();
        existing.setTicketId(ticketId);
        existing.setCategory("BILLING");
        existing.setStatus("OPEN");
        when(repository.findByTicketId(ticketId)).thenReturn(existing);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        String payload = """
                {"eventType": "sla.breached", "ticketId": "%s"}
                """.formatted(ticketId);
        listener.handleEvent(payload);

        org.mockito.ArgumentCaptor<TicketMetric> captor = org.mockito.ArgumentCaptor.forClass(TicketMetric.class);
        verify(repository).save(captor.capture());
        TicketMetric saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.isSlaBreached()).isTrue();
        // The breach event must not clobber fields it has no opinion about.
        assertThat(saved.getCategory()).isEqualTo("BILLING");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void unrecognizedEventTypeIsIgnoredAndCreatesNoMetric() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.findByTicketId(ticketId)).thenReturn(null);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        String payload = """
                {"eventType": "ticket.commented", "ticketId": "%s"}
                """.formatted(ticketId);
        listener.handleEvent(payload);

        verify(repository, never()).save(any(TicketMetric.class));
    }

    @Test
    void malformedJsonPropagatesSoTheErrorHandlerCanRetryAndDeadLetterIt() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        // Must throw: KafkaConfig's DefaultErrorHandler retries and then dead-letters whatever
        // escapes the listener. Swallowing it (the old behaviour) lost the event silently.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.handleEvent("not json at all {{{"))
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void nonUuidTicketIdPropagatesAndNeverReachesTheRepository() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> listener.handleEvent("""
                {"eventType": "ticket.created", "ticketId": "not-a-uuid"}
                """)).isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void eventTypesAnalyticsDoesNotUseAreIgnoredWithoutError() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        listener.handleEvent("""
                {"eventType": "escalation.approved", "ticketId": "%s"}
                """.formatted(UUID.randomUUID()));

        verify(repository, never()).save(any());
    }

    @Test
    void aiResolvedTicketUpdatedEventSetsAiAutoResolvedAndResolvedAt() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        TicketMetric existing = new TicketMetric();
        existing.setTicketId(ticketId);
        when(repository.findByTicketId(ticketId)).thenReturn(existing);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        listener.handleEvent("""
                {"eventType": "ticket.updated", "ticketId": "%s", "previousStatus": "OPEN",
                 "newStatus": "RESOLVED", "aiResolved": true}
                """.formatted(ticketId));

        org.mockito.ArgumentCaptor<TicketMetric> captor = org.mockito.ArgumentCaptor.forClass(TicketMetric.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isAiAutoResolved()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo("RESOLVED");
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
    }

    @Test
    void humanResolvedTicketUpdatedEventDoesNotCountAsAiResolved() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.findByTicketId(ticketId)).thenReturn(null);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        listener.handleEvent("""
                {"eventType": "ticket.updated", "ticketId": "%s", "newStatus": "RESOLVED", "aiResolved": false}
                """.formatted(ticketId));

        org.mockito.ArgumentCaptor<TicketMetric> captor = org.mockito.ArgumentCaptor.forClass(TicketMetric.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isAiAutoResolved()).isFalse();
    }
}
