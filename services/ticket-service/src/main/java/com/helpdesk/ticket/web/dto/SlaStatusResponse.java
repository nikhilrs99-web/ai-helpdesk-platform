package com.helpdesk.ticket.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Computed on demand from sla_targets + the ticket's own createdAt/status - there is no
 * stored "breach" flag on Ticket itself (see SlaBreachJob for the separate, async
 * sla.breached-event side of this).
 */
public record SlaStatusResponse(
        UUID ticketId,
        String slaType,
        boolean targetConfigured,
        Integer targetMinutes,
        Instant deadline,
        boolean breached,
        Long minutesRemaining
) {
}
