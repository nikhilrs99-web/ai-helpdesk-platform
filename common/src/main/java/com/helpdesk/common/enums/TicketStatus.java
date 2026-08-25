package com.helpdesk.common.enums;

/**
 * The ticket lifecycle. Legal transitions between these values are enforced by the State
 * pattern in ticket-service (Week 2), not by this enum itself.
 */
public enum TicketStatus {
    OPEN,
    AI_TRIAGED,
    ASSIGNED,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED
}
