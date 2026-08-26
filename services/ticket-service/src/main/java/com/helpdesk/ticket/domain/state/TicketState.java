package com.helpdesk.ticket.domain.state;

import com.helpdesk.common.enums.TicketStatus;

import java.util.Arrays;
import java.util.Set;

/**
 * State pattern for the ticket lifecycle. Each constant knows only the transitions that
 * are legal from itself, so an illegal transition (e.g. OPEN straight to RESOLVED) is
 * rejected here rather than relying on every call site remembering to check.
 * <p>
 * {@code TicketStatus} (in common) remains the persisted value - this enum is the
 * business-logic layer that validates and computes transitions between those values,
 * looked up via {@link #forStatus(TicketStatus)}.
 */
public enum TicketState {

    OPEN(TicketStatus.OPEN) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(TicketStatus.AI_TRIAGED, TicketStatus.CLOSED);
        }
    },
    AI_TRIAGED(TicketStatus.AI_TRIAGED) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(TicketStatus.ASSIGNED, TicketStatus.CLOSED);
        }
    },
    ASSIGNED(TicketStatus.ASSIGNED) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
        }
    },
    IN_PROGRESS(TicketStatus.IN_PROGRESS) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(TicketStatus.WAITING_FOR_CUSTOMER, TicketStatus.RESOLVED, TicketStatus.CLOSED);
        }
    },
    WAITING_FOR_CUSTOMER(TicketStatus.WAITING_FOR_CUSTOMER) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED);
        }
    },
    RESOLVED(TicketStatus.RESOLVED) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            // IN_PROGRESS is a reopen: the customer says the issue isn't actually fixed.
            return Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
        }
    },
    CLOSED(TicketStatus.CLOSED) {
        @Override
        Set<TicketStatus> legalNextStatuses() {
            return Set.of(); // terminal - a closed ticket cannot transition anywhere
        }
    };

    private final TicketStatus status;

    TicketState(TicketStatus status) {
        this.status = status;
    }

    public TicketStatus status() {
        return status;
    }

    abstract Set<TicketStatus> legalNextStatuses();

    /**
     * @throws IllegalTicketTransitionException if {@code target} is not legal from this state
     */
    public TicketState transitionTo(TicketStatus target) {
        if (!legalNextStatuses().contains(target)) {
            throw new IllegalTicketTransitionException(this.status, target);
        }
        return forStatus(target);
    }

    public static TicketState forStatus(TicketStatus status) {
        return Arrays.stream(values())
                .filter(state -> state.status == status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No TicketState mapped for " + status));
    }
}
