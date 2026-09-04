package com.helpdesk.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    public TicketEventKafkaListener(NotificationDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ticket-events", groupId = "notification-group")
    public void handleTicketEvent(String payload) {
        try {
            // Very simplistic event type extraction for now
            if (payload.contains("\"ticket.created\"")) {
                TicketCreatedEvent event = objectMapper.readValue(payload, TicketCreatedEvent.class);
                
                String eventId = event.eventId().toString();
                if (processedEvents.add(eventId)) {
                    log.info("Received ticket created event from Kafka: {}", eventId);
                    dispatcher.dispatch(event);
                } else {
                    log.info("Ignoring duplicate ticket created event: {}", eventId);
                }
            } else {
                log.debug("Ignoring unsupported event: {}", payload);
            }
        } catch (Exception e) {
            log.error("Failed to process event payload: {}", payload, e);
        }
    }
}
