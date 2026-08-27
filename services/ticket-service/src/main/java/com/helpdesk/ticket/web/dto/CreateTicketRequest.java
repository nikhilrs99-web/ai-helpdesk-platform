package com.helpdesk.ticket.web.dto;

import com.helpdesk.common.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String description,
        @NotNull TicketCategory category,
        Map<String, String> metadata
) {
    public CreateTicketRequest {
        // Which keys are required varies per category (that's exactly why it's the
        // TicketTypeHandler's job, not a fixed Bean Validation annotation here) - but a
        // null map would still make every handler null-check defensively, so normalize once.
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
