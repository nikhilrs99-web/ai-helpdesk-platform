package com.helpdesk.analytics.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    
    private final TicketMetricRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsKafkaListener(TicketMetricRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ticket-events", groupId = "analytics-group")
    public void handleEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText();
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
            } else if ("sla.breached".equals(eventType)) {
                metric.setSlaBreached(true);
            }
            
            repository.save(metric);
            log.info("Persisted analytics for ticket: {}", ticketId);
        } catch (Exception e) {
            log.error("Failed to process event for analytics", e);
        }
    }
}
