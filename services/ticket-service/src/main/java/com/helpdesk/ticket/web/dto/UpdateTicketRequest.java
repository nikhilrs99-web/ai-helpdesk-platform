package com.helpdesk.ticket.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String description
) {
}
