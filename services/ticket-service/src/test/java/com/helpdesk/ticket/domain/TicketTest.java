package com.helpdesk.ticket.domain;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.state.IllegalTicketTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private Ticket newTicket() {
        Ticket ticket = new Ticket();
        ticket.setSubject("Can't log in");
        ticket.setDescription("Password reset link never arrives");
        ticket.setCategory(TicketCategory.ACCESS);
        ticket.setRequesterId("keycloak-subject-customer-1");
        return ticket;
    }

    @Test
    void newTicketDefaultsToOpen() {
        assertThat(newTicket().getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void changeStatusMovesThroughALegalTransition() {
        Ticket ticket = newTicket();

        ticket.changeStatus(TicketStatus.AI_TRIAGED);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AI_TRIAGED);
    }

    @Test
    void changeStatusRejectsSkippingStraightToResolved() {
        Ticket ticket = newTicket();

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.RESOLVED))
                .isInstanceOf(IllegalTicketTransitionException.class);
    }

    @Test
    void aFailedTransitionLeavesTheStatusUnchanged() {
        Ticket ticket = newTicket();
        ticket.changeStatus(TicketStatus.AI_TRIAGED);

        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.WAITING_FOR_CUSTOMER))
                .isInstanceOf(IllegalTicketTransitionException.class);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AI_TRIAGED);
    }
}
