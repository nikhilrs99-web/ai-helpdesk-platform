package com.helpdesk.ticket.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEscalationRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
