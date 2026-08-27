package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DefaultTicketTypeHandlerTest {

    private final DefaultTicketTypeHandler handler = new DefaultTicketTypeHandler();

    @ParameterizedTest
    @EnumSource(value = TicketCategory.class, names = {"ACCESS", "HOW_TO", "FEATURE_REQUEST"})
    void requiresNoMetadataForAnySupportedCategory(TicketCategory category) {
        Ticket ticket = new Ticket();

        assertThatCode(() -> handler.handle(ticket, Map.of())).doesNotThrowAnyException();
    }

    @Test
    void supportsExactlyTheThreeCategoriesWithNoSpecialRequirements() {
        assertThat(handler.supportedCategories())
                .containsExactlyInAnyOrder(TicketCategory.ACCESS, TicketCategory.HOW_TO, TicketCategory.FEATURE_REQUEST);
    }
}
