package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * ACCESS, HOW_TO, and FEATURE_REQUEST have no category-specific requirements today, so
 * they deliberately share one handler instead of three near-empty classes that would just
 * be boilerplate - the Factory only needs a distinct class where behavior actually differs.
 */
@Component
public class DefaultTicketTypeHandler implements TicketTypeHandler {

    @Override
    public Set<TicketCategory> supportedCategories() {
        return Set.of(TicketCategory.ACCESS, TicketCategory.HOW_TO, TicketCategory.FEATURE_REQUEST);
    }

    @Override
    public void handle(Ticket ticket, Map<String, String> metadata) {
        ticket.setMetadata(metadata);
    }
}
