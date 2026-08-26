package com.helpdesk.ticket.exception;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(UUID id) {
        super("No ticket found with id " + id);
    }
}
