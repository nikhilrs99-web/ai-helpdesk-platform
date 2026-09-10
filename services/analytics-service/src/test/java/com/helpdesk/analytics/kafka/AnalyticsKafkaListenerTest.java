package com.helpdesk.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.analytics.domain.TicketMetric;
import com.helpdesk.analytics.domain.TicketMetricRepository;
import org.junit.jupiter.api.Test;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void unrecognizedEventTypeStillPersistsAFoundOrCreatedMetric() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.findByTicketId(ticketId)).thenReturn(null);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        String payload = """
                {"eventType": "ticket.commented", "ticketId": "%s"}
                """.formatted(ticketId);
        listener.handleEvent(payload);

        verify(repository, times(1)).save(any(TicketMetric.class));
    }

    @Test
    void malformedJsonIsSwallowedAndNeverReachesTheRepository() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        // A @KafkaListener method throwing sends the message to the DefaultErrorHandler /
        // dead-letter topic (see KafkaConfig) instead of just logging it - handleEvent's
        // try/catch means garbage payloads are deliberately logged and dropped instead.
        listener.handleEvent("not json at all {{{");

        verify(repository, never()).save(any());
    }

    @Test
    void nonUuidTicketIdIsSwallowedAndNeverReachesTheRepository() {
        TicketMetricRepository repository = mock(TicketMetricRepository.class);
        AnalyticsKafkaListener listener = new AnalyticsKafkaListener(repository, objectMapper);

        String payload = """
                {"eventType": "ticket.created", "ticketId": "not-a-uuid"}
                """;
        listener.handleEvent(payload);

        verify(repository, never()).save(any());
    }
}
