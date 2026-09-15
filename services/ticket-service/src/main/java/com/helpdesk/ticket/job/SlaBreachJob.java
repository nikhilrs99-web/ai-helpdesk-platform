package com.helpdesk.ticket.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.ticket.domain.Sla;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import com.helpdesk.ticket.repository.SlaRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Same target lookup as TicketController.slaStatus (GET /api/tickets/{id}/sla-status) - that
 * endpoint answers "what's the status right now" on demand; this job is what actually
 * detects a breach as it happens and publishes the sla.breached event notification-service
 * and analytics-service both consume.
 */
@Component
public class SlaBreachJob {

    private static final Logger log = LoggerFactory.getLogger(SlaBreachJob.class);
    private static final List<TicketStatus> TERMINAL_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
    private static final String SLA_TYPE = "FIRST_RESPONSE";

    private final TicketRepository ticketRepository;
    private final SlaRepository slaRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public SlaBreachJob(TicketRepository ticketRepository, SlaRepository slaRepository,
                         OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.slaRepository = slaRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void detectSlaBreaches() {
        List<Ticket> candidates = ticketRepository.findBySlaBreachNotifiedFalseAndStatusNotIn(TERMINAL_STATUSES);
        Instant now = Instant.now();
        int breached = 0;

        for (Ticket ticket : candidates) {
            Optional<Sla> target = slaRepository.findByCategoryAndSlaType(ticket.getCategory(), SLA_TYPE);
            if (target.isEmpty()) {
                continue;
            }

            Instant deadline = ticket.getCreatedAt().plus(Duration.ofMinutes(target.get().getTargetMinutes()));
            if (now.isAfter(deadline)) {
                publishBreach(ticket, deadline);
                ticket.markSlaBreachNotified();
                ticketRepository.save(ticket);
                breached++;
            }
        }

        log.info("SLA breach detection: checked {} open ticket(s), published {} sla.breached event(s)",
                candidates.size(), breached);
    }

    private void publishBreach(Ticket ticket, Instant deadline) {
        SlaBreachedEvent event = new SlaBreachedEvent(
                UUID.randomUUID(),
                SlaBreachedEvent.CURRENT_VERSION,
                Instant.now(),
                ticket.getId(),
                SLA_TYPE,
                deadline
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(
                    event.eventId(), "Ticket", ticket.getId().toString(), event.eventType(), payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize sla.breached event for ticket " + ticket.getId(), e);
        }
    }
}
