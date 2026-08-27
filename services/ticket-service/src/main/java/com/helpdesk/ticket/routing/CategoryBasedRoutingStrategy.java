package com.helpdesk.ticket.routing;

import com.helpdesk.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

/**
 * The default routing policy: one team per ticket category. Uses a switch expression
 * rather than a Map&lt;TicketCategory, String&gt; deliberately - a switch over an enum
 * with no default branch is exhaustive at compile time, so adding a new TicketCategory
 * value later fails the build until this is updated, instead of silently falling back
 * to a default team (or throwing at runtime) for a category nobody remembered to map.
 */
@Component
public class CategoryBasedRoutingStrategy implements RoutingStrategy {

    @Override
    public String determineTeam(Ticket ticket) {
        return switch (ticket.getCategory()) {
            case BUG -> "engineering";
            case BILLING -> "billing";
            case ACCESS -> "support";
            case HOW_TO -> "support";
            case FEATURE_REQUEST -> "product";
        };
    }
}
