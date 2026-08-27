package com.helpdesk.ticket.tickettype;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.exception.MissingTicketMetadataException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BugTicketHandlerTest {

    private final BugTicketHandler handler = new BugTicketHandler();

    @Test
    void rejectsMissingBrowserAndAppVersion() {
        Ticket ticket = new Ticket();

        assertThatThrownBy(() -> handler.handle(ticket, Map.of()))
                .isInstanceOf(MissingTicketMetadataException.class)
                .hasMessageContaining("browser")
                .hasMessageContaining("appVersion");
    }

    @Test
    void rejectsWhenOnlyOneRequiredKeyIsPresent() {
        Ticket ticket = new Ticket();

        assertThatThrownBy(() -> handler.handle(ticket, Map.of("browser", "Chrome")))
                .isInstanceOf(MissingTicketMetadataException.class)
                .hasMessageContaining("appVersion")
                .hasMessageNotContaining("browser");
    }

    @Test
    void acceptsCompleteMetadataAndSetsItOnTheTicket() {
        Ticket ticket = new Ticket();
        Map<String, String> metadata = Map.of("browser", "Chrome", "appVersion", "1.2.3");

        handler.handle(ticket, metadata);

        assertThat(ticket.getMetadata()).isEqualTo(metadata);
    }

    @Test
    void onlySupportsBug() {
        assertThat(handler.supportedCategories()).containsExactly(TicketCategory.BUG);
    }
}
