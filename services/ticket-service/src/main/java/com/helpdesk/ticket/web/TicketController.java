package com.helpdesk.ticket.web;

import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.Sla;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.exception.TicketNotFoundException;
import com.helpdesk.ticket.repository.SlaRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.routing.RoutingStrategy;
import com.helpdesk.ticket.tickettype.TicketTypeHandler;
import com.helpdesk.ticket.tickettype.TicketTypeHandlerFactory;
import com.helpdesk.ticket.web.dto.ChangeStatusRequest;
import com.helpdesk.ticket.web.dto.CreateTicketRequest;
import com.helpdesk.ticket.web.dto.SlaStatusResponse;
import com.helpdesk.ticket.web.dto.TicketResponse;
import com.helpdesk.ticket.web.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final RoutingStrategy routingStrategy;
    private final TicketTypeHandlerFactory typeHandlerFactory;
    private final com.helpdesk.ticket.outbox.OutboxRepository outboxRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.helpdesk.ticket.redis.RateLimiterService rateLimiterService;
    private final SlaRepository slaRepository;

    public TicketController(TicketRepository ticketRepository, RoutingStrategy routingStrategy,
                             TicketTypeHandlerFactory typeHandlerFactory,
                             com.helpdesk.ticket.outbox.OutboxRepository outboxRepository,
                             com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                             com.helpdesk.ticket.redis.RateLimiterService rateLimiterService,
                             SlaRepository slaRepository) {
        this.ticketRepository = ticketRepository;
        this.routingStrategy = routingStrategy;
        this.typeHandlerFactory = typeHandlerFactory;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.rateLimiterService = rateLimiterService;
        this.slaRepository = slaRepository;
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        if (!rateLimiterService.isAllowed(userId)) {
            return ResponseEntity.status(429).build();
        }

        Ticket ticket = new Ticket();
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
        ticket.setRequesterId(userId);
        ticket.setRoutedTeam(routingStrategy.determineTeam(ticket));

        // Validates + populates category-specific metadata (Factory pattern); throws
        // MissingTicketMetadataException (-> 400) if a required key for this category is absent.
        TicketTypeHandler handler = typeHandlerFactory.forCategory(request.category());
        handler.handle(ticket, request.metadata());

        Ticket saved = ticketRepository.save(ticket);

        // Day 27: Save to outbox table instead of direct REST call
        try {
            com.helpdesk.common.event.TicketCreatedEvent event = new com.helpdesk.common.event.TicketCreatedEvent(
                    UUID.randomUUID(),
                    com.helpdesk.common.event.TicketCreatedEvent.CURRENT_VERSION,
                    java.time.Instant.now(),
                    saved.getId(),
                    saved.getCategory(),
                    saved.getRequesterId()
            );
            
            String payload = objectMapper.writeValueAsString(event);
            com.helpdesk.ticket.outbox.OutboxEvent outboxEvent = new com.helpdesk.ticket.outbox.OutboxEvent(
                    event.eventId(),
                    "Ticket",
                    saved.getId().toString(),
                    event.eventType(),
                    payload
            );
            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        return ResponseEntity.created(URI.create("/api/tickets/" + saved.getId()))
                .body(TicketResponse.from(saved));
    }

    @PreAuthorize("hasAnyRole('agent','admin') or @ticketSecurity.isOwner(authentication, #id)")
    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id) {
        return TicketResponse.from(findOrThrow(id));
    }

    @GetMapping
    public Page<TicketResponse> list(@RequestParam(required = false) String requesterId,
                                      Pageable pageable, Authentication authentication) {
        // A customer only ever sees their own tickets, regardless of what (if anything) they
        // pass as requesterId - that can't be expressed as a @PreAuthorize gate (all-or-nothing
        // per method call, not row-level), so the scoping happens in which query actually runs.
        if (!isElevated(authentication)) {
            return ticketRepository.findByRequesterId(authentication.getName(), pageable).map(TicketResponse::from);
        }
        // An agent/admin can optionally narrow to one customer's tickets (e.g. the AI agent's
        // getCustomerTickets tool, acting on that agent's own forwarded token) - otherwise sees all.
        if (requesterId != null && !requesterId.isBlank()) {
            return ticketRepository.findByRequesterId(requesterId, pageable).map(TicketResponse::from);
        }
        return ticketRepository.findAll(pageable).map(TicketResponse::from);
    }

    @PreAuthorize("hasAnyRole('agent','admin') or @ticketSecurity.isOwner(authentication, #id)")
    @GetMapping("/{id}/sla-status")
    public SlaStatusResponse slaStatus(@PathVariable UUID id) {
        Ticket ticket = findOrThrow(id);
        Optional<Sla> target = slaRepository.findByCategoryAndSlaType(ticket.getCategory(), "FIRST_RESPONSE");

        if (target.isEmpty()) {
            return new SlaStatusResponse(id, "FIRST_RESPONSE", false, null, null, false, null);
        }

        Instant deadline = ticket.getCreatedAt().plus(Duration.ofMinutes(target.get().getTargetMinutes()));
        boolean resolved = ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED;
        boolean breached = !resolved && Instant.now().isAfter(deadline);
        long minutesRemaining = Duration.between(Instant.now(), deadline).toMinutes();

        return new SlaStatusResponse(id, "FIRST_RESPONSE", true, target.get().getTargetMinutes(),
                deadline, breached, minutesRemaining);
    }

    @PreAuthorize("hasAnyRole('agent','admin') or @ticketSecurity.isOwner(authentication, #id)")
    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        Ticket ticket = findOrThrow(id);
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        // findById and save each run in their own transaction (open-in-view is disabled), so the
        // ticket is detached by the time we mutate it here - save() is what actually persists this.
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @PreAuthorize("hasAnyRole('agent','admin')")
    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        Ticket ticket = findOrThrow(id);
        ticket.changeStatus(request.status());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket findOrThrow(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private boolean isElevated(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_agent") || authority.equals("ROLE_admin"));
    }
}
