package com.helpdesk.ticket.web.dto;

import com.helpdesk.common.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull TicketStatus status
) {
}
