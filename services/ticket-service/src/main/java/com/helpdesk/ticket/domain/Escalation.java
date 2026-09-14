package com.helpdesk.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * The real, persisted "human-in-the-loop" gate behind ai-service's createEscalation tool
 * (see docs/rag/agent-tools.md): a PENDING row created here has no effect on its own - only
 * an agent/admin calling the approve endpoint (EscalationController) commits it.
 */
@Entity
@Table(name = "escalations")
public class Escalation extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(nullable = false, length = 1000)
    private String reason;

    // The Keycloak subject that asked for the escalation - the end user chatting with the AI
    // agent, not ai-service itself (see AgentToolsConfig: tools forward the caller's own
    // token, they don't act under a separate service identity).
    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationStatus status = EscalationStatus.PENDING;

    @Column(name = "decided_by")
    private String decidedBy;

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public EscalationStatus getStatus() {
        return status;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void approve(String decidedBy) {
        this.status = EscalationStatus.APPROVED;
        this.decidedBy = decidedBy;
    }

    public void reject(String decidedBy) {
        this.status = EscalationStatus.REJECTED;
        this.decidedBy = decidedBy;
    }
}
