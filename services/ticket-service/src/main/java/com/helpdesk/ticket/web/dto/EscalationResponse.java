package com.helpdesk.ticket.web.dto;

import com.helpdesk.ticket.domain.Escalation;
import com.helpdesk.ticket.domain.EscalationStatus;

import java.time.Instant;
import java.util.UUID;

public record EscalationResponse(
        UUID id,
        UUID ticketId,
        String reason,
        String requestedBy,
        EscalationStatus status,
        String decidedBy,
        Instant createdAt
) {
    public static EscalationResponse from(Escalation escalation) {
        return new EscalationResponse(
                escalation.getId(),
                escalation.getTicketId(),
                escalation.getReason(),
                escalation.getRequestedBy(),
                escalation.getStatus(),
                escalation.getDecidedBy(),
                escalation.getCreatedAt()
        );
    }
}
