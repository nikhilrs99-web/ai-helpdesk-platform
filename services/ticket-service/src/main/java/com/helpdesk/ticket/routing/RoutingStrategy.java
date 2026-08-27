package com.helpdesk.ticket.routing;

import com.helpdesk.ticket.domain.Ticket;

/**
 * Strategy pattern for deciding which team a ticket is routed to. Interchangeable by
 * design - e.g. a future AI-driven strategy (Phase 5) can implement this same interface
 * and be swapped in via dependency injection without TicketController changing at all.
 */
public interface RoutingStrategy {

    String determineTeam(Ticket ticket);
}
