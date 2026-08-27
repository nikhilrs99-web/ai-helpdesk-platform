package com.helpdesk.ticket.tickettype;

import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.exception.MissingTicketMetadataException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingTicketHandlerTest {

    private final BillingTicketHandler handler = new BillingTicketHandler();

    @Test
    void rejectsMissingInvoiceId() {
        Ticket ticket = new Ticket();

        assertThatThrownBy(() -> handler.handle(ticket, Map.of()))
                .isInstanceOf(MissingTicketMetadataException.class)
                .hasMessageContaining("invoiceId");
    }

    @Test
    void defaultsCurrencyToUsdWhenNotProvided() {
        Ticket ticket = new Ticket();

        handler.handle(ticket, Map.of("invoiceId", "INV-1042"));

        assertThat(ticket.getMetadata())
                .containsEntry("invoiceId", "INV-1042")
                .containsEntry("currency", "USD");
    }

    @Test
    void keepsAnExplicitlyProvidedCurrency() {
        Ticket ticket = new Ticket();

        handler.handle(ticket, Map.of("invoiceId", "INV-1042", "currency", "EUR"));

        assertThat(ticket.getMetadata()).containsEntry("currency", "EUR");
    }
}
