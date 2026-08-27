package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory pattern: given a category, returns the right handler. Spring injects every
 * TicketTypeHandler bean; the constructor builds the lookup map itself and fails fast at
 * startup - not at request time - if two handlers claim the same category, or if any
 * TicketCategory value has no handler at all. A new category added to the enum without a
 * matching handler breaks application startup immediately, the same "catch it as early as
 * possible" spirit as Day 11's exhaustive switch expression.
 */
@Component
public class TicketTypeHandlerFactory {

    private final Map<TicketCategory, TicketTypeHandler> handlersByCategory;

    public TicketTypeHandlerFactory(List<TicketTypeHandler> handlers) {
        Map<TicketCategory, TicketTypeHandler> map = new EnumMap<>(TicketCategory.class);
        for (TicketTypeHandler handler : handlers) {
            for (TicketCategory category : handler.supportedCategories()) {
                TicketTypeHandler existing = map.put(category, handler);
                if (existing != null) {
                    throw new IllegalStateException(
                            "Multiple TicketTypeHandlers registered for category " + category + ": "
                                    + existing.getClass().getSimpleName() + " and " + handler.getClass().getSimpleName());
                }
            }
        }
        for (TicketCategory category : TicketCategory.values()) {
            if (!map.containsKey(category)) {
                throw new IllegalStateException("No TicketTypeHandler registered for category " + category);
            }
        }
        this.handlersByCategory = map;
    }

    public TicketTypeHandler forCategory(TicketCategory category) {
        return handlersByCategory.get(category);
    }
}
