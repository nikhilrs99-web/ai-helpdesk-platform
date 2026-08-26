package com.helpdesk.ticket.domain.state;

import com.helpdesk.common.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStateTest {

    @Test
    void allowsTheFullHappyPathLifecycle() {
        TicketState state = TicketState.OPEN;
        state = state.transitionTo(TicketStatus.AI_TRIAGED);
        state = state.transitionTo(TicketStatus.ASSIGNED);
        state = state.transitionTo(TicketStatus.IN_PROGRESS);
        state = state.transitionTo(TicketStatus.WAITING_FOR_CUSTOMER);
        state = state.transitionTo(TicketStatus.RESOLVED);
        state = state.transitionTo(TicketStatus.CLOSED);

        assertThat(state.status()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void rejectsSkippingStraightFromOpenToResolved() {
        assertThatThrownBy(() -> TicketState.OPEN.transitionTo(TicketStatus.RESOLVED))
                .isInstanceOf(IllegalTicketTransitionException.class)
                .hasMessageContaining("OPEN")
                .hasMessageContaining("RESOLVED");
    }

    @Test
    void allowsResolvedToBeReopenedIntoInProgress() {
        TicketState reopened = TicketState.RESOLVED.transitionTo(TicketStatus.IN_PROGRESS);

        assertThat(reopened.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void allowsAnyNonTerminalStateToCloseEarly() {
        assertThat(TicketState.OPEN.transitionTo(TicketStatus.CLOSED).status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(TicketState.AI_TRIAGED.transitionTo(TicketStatus.CLOSED).status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(TicketState.IN_PROGRESS.transitionTo(TicketStatus.CLOSED).status()).isEqualTo(TicketStatus.CLOSED);
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void closedIsTerminalForEveryPossibleTarget(TicketStatus target) {
        assertThatThrownBy(() -> TicketState.CLOSED.transitionTo(target))
                .isInstanceOf(IllegalTicketTransitionException.class);
    }
}
