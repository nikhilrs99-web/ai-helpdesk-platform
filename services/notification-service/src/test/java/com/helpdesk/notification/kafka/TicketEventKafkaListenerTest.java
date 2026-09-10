package com.helpdesk.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.event.TicketCreatedEvent;
import com.helpdesk.notification.observer.NotificationDispatcher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Same rationale as AnalyticsKafkaListenerTest: @KafkaListener just routes a raw payload to
 * this method, so calling it directly proves the parsing/dedup/dispatch logic without an
 * embedded broker. The in-memory processedEvents set is this class's whole reason to have
 * state, so the duplicate-delivery case is the one most worth pinning down.
 */
class TicketEventKafkaListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String ticketCreatedPayload(UUID eventId, UUID ticketId) throws Exception {
        TicketCreatedEvent event = new TicketCreatedEvent(
                eventId, TicketCreatedEvent.CURRENT_VERSION, Instant.now(), ticketId,
                TicketCategory.ACCESS, "requester-1");
        return objectMapper.writeValueAsString(event);
    }

    @Test
    void ticketCreatedEventIsDispatchedExactlyOnce() throws Exception {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        TicketEventKafkaListener listener = new TicketEventKafkaListener(dispatcher, objectMapper);
        String payload = ticketCreatedPayload(UUID.randomUUID(), UUID.randomUUID());

        listener.handleTicketEvent(payload);

        verify(dispatcher, times(1)).dispatch(any(TicketCreatedEvent.class));
    }

    @Test
    void redeliveredEventWithSameIdIsNotDispatchedTwice() throws Exception {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        TicketEventKafkaListener listener = new TicketEventKafkaListener(dispatcher, objectMapper);
        UUID eventId = UUID.randomUUID();
        String payload = ticketCreatedPayload(eventId, UUID.randomUUID());

        // Kafka's at-least-once delivery means the exact same message can arrive twice -
        // this is the in-memory idempotency check's actual job.
        listener.handleTicketEvent(payload);
        listener.handleTicketEvent(payload);

        verify(dispatcher, times(1)).dispatch(any(TicketCreatedEvent.class));
    }

    @Test
    void unsupportedEventTypeIsIgnored() {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        TicketEventKafkaListener listener = new TicketEventKafkaListener(dispatcher, objectMapper);

        listener.handleTicketEvent("{\"eventType\": \"sla.breached\", \"ticketId\": \"whatever\"}");

        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void malformedPayloadIsSwallowedAndNeverReachesTheDispatcher() {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        TicketEventKafkaListener listener = new TicketEventKafkaListener(dispatcher, objectMapper);

        // Contains the ticket.created marker the listener string-matches on, but isn't valid
        // TicketCreatedEvent JSON - proves the parse failure is caught, not just the branch
        // that never gets there.
        listener.handleTicketEvent("{\"eventType\": \"ticket.created\", not even valid json");

        verify(dispatcher, never()).dispatch(any());
    }
}
