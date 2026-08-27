package com.helpdesk.ticket.exception;

import com.helpdesk.common.enums.TicketCategory;

import java.util.Set;

public class MissingTicketMetadataException extends RuntimeException {

    public MissingTicketMetadataException(TicketCategory category, Set<String> missingKeys) {
        super("Missing required metadata for category %s: %s".formatted(category, missingKeys));
    }
}
