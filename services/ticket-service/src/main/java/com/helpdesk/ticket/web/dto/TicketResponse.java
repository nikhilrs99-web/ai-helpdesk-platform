package com.helpdesk.ticket.web.dto;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.Ticket;

import java.time.Instant;
import java.util.UUID;

/**
 * The API's own shape for a ticket, deliberately separate from the Ticket entity. Keeps the
 * public contract stable even if the JPA entity's internals change, and avoids ever
 * serializing a lazy-loaded JPA proxy directly over the wire.
 */
public record TicketResponse(
        UUID id,
        String subject,
        String description,
        TicketCategory category,
        TicketStatus status,
        String requesterId,
        UUID assignedAgentId,
        String routedTeam,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getRequesterId(),
                ticket.getAssignedAgent() == null ? null : ticket.getAssignedAgent().getId(),
                ticket.getRoutedTeam(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
