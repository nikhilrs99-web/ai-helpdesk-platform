package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.exception.MissingTicketMetadataException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BugTicketHandler implements TicketTypeHandler {

    private static final Set<String> REQUIRED_KEYS = Set.of("browser", "appVersion");

    @Override
    public Set<TicketCategory> supportedCategories() {
        return Set.of(TicketCategory.BUG);
    }

    @Override
    public void handle(Ticket ticket, Map<String, String> metadata) {
        Set<String> missing = REQUIRED_KEYS.stream()
                .filter(key -> !metadata.containsKey(key))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            throw new MissingTicketMetadataException(TicketCategory.BUG, missing);
        }
        ticket.setMetadata(metadata);
    }
}
