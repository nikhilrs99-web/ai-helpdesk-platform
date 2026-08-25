package com.helpdesk.common.enums;

/**
 * Drives the Factory pattern in ticket-service (Week 3): each category maps to a handler
 * with different required fields and different routing behavior.
 */
public enum TicketCategory {
    BUG,
    BILLING,
    ACCESS,
    HOW_TO,
    FEATURE_REQUEST
}
