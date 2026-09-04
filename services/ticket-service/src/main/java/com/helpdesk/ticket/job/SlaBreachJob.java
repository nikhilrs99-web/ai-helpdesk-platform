package com.helpdesk.ticket.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class SlaBreachJob {

    private static final Logger log = LoggerFactory.getLogger(SlaBreachJob.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public SlaBreachJob(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void detectSlaBreaches() {
        log.info("Running SLA Breach detection job...");
        // In a real scenario, query ticketRepository for OPEN tickets past their SLA deadline.
        // For Day 33 demonstration, we publish a structural SLA breached event via the Outbox.
        
        try {
            String fakeTicketId = UUID.randomUUID().toString();
            String payload = String.format("{\"eventId\":\"%s\", \"eventType\":\"sla.breached\", \"ticketId\":\"%s\", \"occurredAt\":\"%s\"}", 
                    UUID.randomUUID(), fakeTicketId, Instant.now().toString());
            
            OutboxEvent event = new OutboxEvent(
                    UUID.randomUUID(),
                    "Ticket",
                    fakeTicketId,
                    "sla.breached",
                    payload
            );
            outboxRepository.save(event);
            log.info("Published sla.breached event for ticket {} to outbox", fakeTicketId);
        } catch (Exception e) {
            log.error("Failed to publish SLA breach event", e);
        }
    }
}
