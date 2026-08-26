package com.helpdesk.ticket.web.dto;

import com.helpdesk.common.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String description,
        @NotNull TicketCategory category
) {
}
