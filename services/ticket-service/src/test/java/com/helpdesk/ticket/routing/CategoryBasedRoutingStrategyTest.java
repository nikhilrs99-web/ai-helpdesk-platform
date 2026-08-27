package com.helpdesk.ticket.routing;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryBasedRoutingStrategyTest {

    private final CategoryBasedRoutingStrategy strategy = new CategoryBasedRoutingStrategy();

    @ParameterizedTest
    @CsvSource({
            "BUG, engineering",
            "BILLING, billing",
            "ACCESS, support",
            "HOW_TO, support",
            "FEATURE_REQUEST, product"
    })
    void routesEachCategoryToItsTeam(TicketCategory category, String expectedTeam) {
        Ticket ticket = new Ticket();
        ticket.setCategory(category);

        assertThat(strategy.determineTeam(ticket)).isEqualTo(expectedTeam);
    }
}
