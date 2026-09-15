package com.helpdesk.ticket.web;

import com.helpdesk.common.event.EscalationApprovedEvent;
import com.helpdesk.ticket.domain.Escalation;
import com.helpdesk.ticket.exception.EscalationNotFoundException;
import com.helpdesk.ticket.exception.TicketNotFoundException;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import com.helpdesk.ticket.repository.EscalationRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.web.dto.CreateEscalationRequest;
import com.helpdesk.ticket.web.dto.EscalationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The persisted, gated "human-in-the-loop" workflow behind ai-service's createEscalation
 * tool (docs/rag/agent-tools.md). A PENDING row here is inert - approve/reject are the only
 * state-changing actions, and both are agent/admin-only. Approving is also the point
 * docs/rag/agent-tools.md says notifications get triggered - see approve() below.
 */
@RestController
@RequestMapping("/api/tickets/{ticketId}/escalations")
public class EscalationController {

    private final EscalationRepository escalationRepository;
    private final TicketRepository ticketRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public EscalationController(EscalationRepository escalationRepository, TicketRepository ticketRepository,
                                 OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.escalationRepository = escalationRepository;
        this.ticketRepository = ticketRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAnyRole('agent','admin') or @ticketSecurity.isOwner(authentication, #ticketId)")
    @PostMapping
    public ResponseEntity<EscalationResponse> create(@PathVariable UUID ticketId,
                                                       @Valid @RequestBody CreateEscalationRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        Escalation escalation = new Escalation();
        escalation.setTicketId(ticketId);
        escalation.setReason(request.reason());
        escalation.setRequestedBy(jwt.getSubject());
        Escalation saved = escalationRepository.save(escalation);

        return ResponseEntity.created(URI.create("/api/tickets/" + ticketId + "/escalations/" + saved.getId()))
                .body(EscalationResponse.from(saved));
    }

    @PreAuthorize("hasAnyRole('agent','admin') or @ticketSecurity.isOwner(authentication, #ticketId)")
    @GetMapping
    public List<EscalationResponse> list(@PathVariable UUID ticketId) {
        return escalationRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                .map(EscalationResponse::from)
                .toList();
    }

    @PreAuthorize("hasAnyRole('agent','admin')")
    @PatchMapping("/{id}/approve")
    @Transactional
    public EscalationResponse approve(@PathVariable UUID ticketId, @PathVariable UUID id,
                                       @AuthenticationPrincipal Jwt jwt) {
        Escalation escalation = findOrThrow(ticketId, id);
        escalation.approve(jwt.getSubject());
        Escalation saved = escalationRepository.save(escalation);

        // Same outbox pattern as TicketController.create(): write it in the same transaction
        // as the state change itself, OutboxWorker (already fixed to wait for the broker ack)
        // is what actually gets it to Kafka.
        EscalationApprovedEvent event = new EscalationApprovedEvent(
                UUID.randomUUID(), EscalationApprovedEvent.CURRENT_VERSION, Instant.now(),
                ticketId, saved.getId(), saved.getReason(), saved.getDecidedBy());
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(new OutboxEvent(
                    event.eventId(), "Ticket", ticketId.toString(), event.eventType(), payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize escalation.approved event for ticket " + ticketId, e);
        }

        return EscalationResponse.from(saved);
    }

    @PreAuthorize("hasAnyRole('agent','admin')")
    @PatchMapping("/{id}/reject")
    public EscalationResponse reject(@PathVariable UUID ticketId, @PathVariable UUID id,
                                      @AuthenticationPrincipal Jwt jwt) {
        Escalation escalation = findOrThrow(ticketId, id);
        escalation.reject(jwt.getSubject());
        return EscalationResponse.from(escalationRepository.save(escalation));
    }

    private Escalation findOrThrow(UUID ticketId, UUID id) {
        Escalation escalation = escalationRepository.findById(id)
                .orElseThrow(() -> new EscalationNotFoundException(id));
        if (!escalation.getTicketId().equals(ticketId)) {
            throw new EscalationNotFoundException(id);
        }
        return escalation;
    }
}
