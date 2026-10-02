package com.helpdesk.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import com.helpdesk.notification.observer.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TicketEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventKafkaListener.class);

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    // In-memory idempotency check (for temporary use before Redis/DB deduplication)
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    // Every event's own eventType field is what actually routes it - reading this first,
    // separately from the concrete type, is what makes the switch below possible instead of
    // the previous payload.contains("\"ticket.created\"") substring guess.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EventEnvelope(String eventType) {}

    public TicketEventKafkaListener(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    // No longer catches-and-swallows every exception: a deserialization failure or a
    // dispatch problem now propagates so the DefaultErrorHandler + DeadLetterPublishingRecoverer
    // configured in KafkaConfig can actually retry and DLQ it - that machinery was configured
    // but unreachable before, since nothing ever escaped this method.
    @KafkaListener(topics = "ticket-events", groupId = "notification-group")
    public void handleTicketEvent(String payload) throws JsonProcessingException {
        String eventType = objectMapper.readValue(payload, EventEnvelope.class).eventType();

        DomainEvent event = switch (eventType) {
            case TicketCreatedEvent.EVENT_TYPE -> objectMapper.readValue(payload, TicketCreatedEvent.class);
            case SlaBreachedEvent.EVENT_TYPE -> objectMapper.readValue(payload, SlaBreachedEvent.class);
            // Forward-compatible on purpose (see docs/kafka/event-schema.md's versioning
            // policy): a future event type this consumer doesn't know about yet is ignored,
            // not an error.
            default -> null;
        };

        if (event == null) {
            log.debug("Ignoring unsupported event type: {}", eventType);
            return;
        }

        String eventId = event.eventId().toString();
        if (processedEvents.add(eventId)) {
            log.info("Received {} event from Kafka: {}", eventType, eventId);
            dispatcher.dispatch(event);
        } else {
            log.info("Ignoring duplicate {} event: {}", eventType, eventId);
        }
    }
}
