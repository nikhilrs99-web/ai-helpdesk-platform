package com.helpdesk.ticket.web;

import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.exception.TicketNotFoundException;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.web.dto.ChangeStatusRequest;
import com.helpdesk.ticket.web.dto.CreateTicketRequest;
import com.helpdesk.ticket.web.dto.TicketResponse;
import com.helpdesk.ticket.web.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        Ticket ticket = new Ticket();
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
        // Never trust a client-supplied requester id - always derive it from the authenticated token.
        ticket.setRequesterId(jwt.getSubject());

        Ticket saved = ticketRepository.save(ticket);

        return ResponseEntity.created(URI.create("/api/tickets/" + saved.getId()))
                .body(TicketResponse.from(saved));
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id) {
        return TicketResponse.from(findOrThrow(id));
    }

    @GetMapping
    public Page<TicketResponse> list(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(TicketResponse::from);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        Ticket ticket = findOrThrow(id);
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        // findById and save each run in their own transaction (open-in-view is disabled), so the
        // ticket is detached by the time we mutate it here - save() is what actually persists this.
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        Ticket ticket = findOrThrow(id);
        ticket.changeStatus(request.status());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket findOrThrow(UUID id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
