package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTypeHandlerFactoryTest {

    private static final List<TicketTypeHandler> REAL_HANDLERS =
            List.of(new BugTicketHandler(), new BillingTicketHandler(), new DefaultTicketTypeHandler());

    @Test
    void resolvesEveryCategoryToItsRightHandler() {
        TicketTypeHandlerFactory factory = new TicketTypeHandlerFactory(REAL_HANDLERS);

        assertThat(factory.forCategory(TicketCategory.BUG)).isInstanceOf(BugTicketHandler.class);
        assertThat(factory.forCategory(TicketCategory.BILLING)).isInstanceOf(BillingTicketHandler.class);
        assertThat(factory.forCategory(TicketCategory.ACCESS)).isInstanceOf(DefaultTicketTypeHandler.class);
        assertThat(factory.forCategory(TicketCategory.HOW_TO)).isInstanceOf(DefaultTicketTypeHandler.class);
        assertThat(factory.forCategory(TicketCategory.FEATURE_REQUEST)).isInstanceOf(DefaultTicketTypeHandler.class);
    }

    @Test
    void failsFastAtConstructionIfACategoryHasNoHandler() {
        // Missing a handler for BILLING and FEATURE_REQUEST entirely.
        List<TicketTypeHandler> incomplete = List.of(new BugTicketHandler(), new AccessOnlyHandler());

        assertThatThrownBy(() -> new TicketTypeHandlerFactory(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No TicketTypeHandler registered");
    }

    @Test
    void failsFastAtConstructionIfTwoHandlersClaimTheSameCategory() {
        List<TicketTypeHandler> duplicated = List.of(new BugTicketHandler(), new AlsoBugHandler(),
                new BillingTicketHandler(), new DefaultTicketTypeHandler());

        assertThatThrownBy(() -> new TicketTypeHandlerFactory(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple TicketTypeHandlers registered");
    }

    /** Deliberately incomplete test double - only handles ACCESS, to prove missing coverage is caught. */
    private static class AccessOnlyHandler implements TicketTypeHandler {
        @Override
        public Set<TicketCategory> supportedCategories() {
            return Set.of(TicketCategory.ACCESS);
        }

        @Override
        public void handle(Ticket ticket, Map<String, String> metadata) {
            ticket.setMetadata(metadata);
        }
    }

    /** A second handler that also claims BUG, to prove duplicate registration is caught. */
    private static class AlsoBugHandler implements TicketTypeHandler {
        @Override
        public Set<TicketCategory> supportedCategories() {
            return Set.of(TicketCategory.BUG);
        }

        @Override
        public void handle(Ticket ticket, Map<String, String> metadata) {
            ticket.setMetadata(metadata);
        }
    }
}
