package com.helpdesk.ticket.security;

import com.helpdesk.ticket.repository.TicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Backs the "or owns the ticket" half of the @PreAuthorize checks on TicketController.
 * Ownership can't be read off the request alone (ticket ids are opaque UUIDs), so this
 * does the one lightweight lookup needed to answer "does this ticket belong to this
 * caller?" before the controller method body runs - unlike @PostAuthorize, this can gate
 * mutating endpoints (update, changeStatus) without letting the write happen first.
 */
@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;

    public TicketSecurity(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public boolean isOwner(Authentication authentication, UUID ticketId) {
        return ticketRepository.existsByIdAndRequesterId(ticketId, authentication.getName());
    }
}
