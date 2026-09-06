package com.helpdesk.analytics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_metrics")
public class TicketMetric {
    @Id
    private UUID id;
    private UUID ticketId;
    private String category;
    private String status;
    private boolean slaBreached;
    private boolean aiAutoResolved;
    private Instant createdAt;
    private Instant resolvedAt;

    public TicketMetric() {
        this.id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }
    public boolean isAiAutoResolved() { return aiAutoResolved; }
    public void setAiAutoResolved(boolean aiAutoResolved) { this.aiAutoResolved = aiAutoResolved; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
