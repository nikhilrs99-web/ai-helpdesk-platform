package com.helpdesk.ticket.domain.state;

import com.helpdesk.common.enums.TicketStatus;

/**
 * Thrown when code tries to move a ticket to a status that isn't legal from its current
 * status - e.g. jumping straight from OPEN to RESOLVED. Unchecked, since a caller should
 * generally know a ticket's current status before attempting a transition.
 */
public class IllegalTicketTransitionException extends RuntimeException {

    public IllegalTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot transition ticket from %s to %s".formatted(from, to));
    }
}
