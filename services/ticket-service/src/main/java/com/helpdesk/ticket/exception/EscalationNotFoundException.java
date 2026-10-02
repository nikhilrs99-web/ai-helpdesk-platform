package com.helpdesk.ticket.exception;

import java.util.UUID;

public class EscalationNotFoundException extends RuntimeException {

    public EscalationNotFoundException(UUID id) {
        super("No escalation found with id " + id);
    }
}
