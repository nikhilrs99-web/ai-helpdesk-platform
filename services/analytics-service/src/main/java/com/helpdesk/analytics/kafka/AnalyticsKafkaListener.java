package com.helpdesk.analytics.kafka;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.helpdesk.analytics.domain.TicketMetric;
import com.helpdesk.analytics.domain.TicketMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AnalyticsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsKafkaListener.class);
    
    private static final java.util.Set<String> HANDLED_EVENT_TYPES =
            java.util.Set.of("ticket.created", "ticket.updated", "sla.breached");

    private final TicketMetricRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsKafkaListener(TicketMetricRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // Deliberately no catch-all: a malformed payload or a DB failure must propagate so
    // KafkaConfig's DefaultErrorHandler can retry it 3x and then publish it to the dead-letter
    // topic. Swallowing the exception here (as this method used to) silently dropped the event
    // and made that retry/DLQ configuration unreachable. Every update below is idempotent
    // (it sets state rather than incrementing), so redelivery is safe.
    @KafkaListener(topics = "ticket-events", groupId = "analytics-group")
    public void handleEvent(String payload) {
        JsonNode node = objectMapper.readTree(payload);
        String eventType = node.path("eventType").asText();
        if (!HANDLED_EVENT_TYPES.contains(eventType)) {
            log.debug("Ignoring event type not used by analytics: {}", eventType);
            return;
        }
        UUID ticketId = UUID.fromString(node.path("ticketId").asText());

        TicketMetric metric = repository.findByTicketId(ticketId);
        if (metric == null) {
            metric = new TicketMetric();
            metric.setTicketId(ticketId);
            metric.setCreatedAt(Instant.now());
        }

        if ("ticket.created".equals(eventType)) {
            metric.setCategory(node.path("category").asText());
            metric.setStatus("OPEN");
        } else if ("ticket.updated".equals(eventType)) {
            String newStatus = node.path("newStatus").asText();
            metric.setStatus(newStatus);
            if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
                metric.setResolvedAt(Instant.now());
            }
            if (node.path("aiResolved").asBoolean(false)) {
                metric.setAiAutoResolved(true);
            }
        } else if ("sla.breached".equals(eventType)) {
            metric.setSlaBreached(true);
        }

        repository.save(metric);
        log.info("Persisted analytics for ticket: {}", ticketId);
    }
}
