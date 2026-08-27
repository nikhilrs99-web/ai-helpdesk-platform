package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;

import java.util.Map;
import java.util.Set;

/**
 * Factory pattern: each concrete handler owns the category-specific rules for one or more
 * TicketCategory values - which metadata keys are required, and any defaults to apply -
 * instead of a single method with a switch/if-else per category duplicated at every call site.
 */
public interface TicketTypeHandler {

    Set<TicketCategory> supportedCategories();

    /**
     * Validates the given metadata against this category's requirements and sets it (with
     * any defaults applied) on the ticket.
     *
     * @throws com.helpdesk.ticket.exception.MissingTicketMetadataException if a required key is absent
     */
    void handle(Ticket ticket, Map<String, String> metadata);
}
