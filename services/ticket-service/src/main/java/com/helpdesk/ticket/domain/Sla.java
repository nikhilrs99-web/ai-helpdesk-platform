package com.helpdesk.ticket.domain;

import com.helpdesk.common.enums.TicketCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * SLA targets by category, e.g. (BILLING, FIRST_RESPONSE, 240 minutes). A reference/config
 * table for now - live breach tracking against these targets is a Kafka-driven feature
 * added later (see docs/kafka/event-schema.md, sla.breached).
 */
@Entity
@Table(name = "sla_targets", uniqueConstraints = @UniqueConstraint(columnNames = {"category", "sla_type"}))
public class Sla extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Column(name = "sla_type", nullable = false)
    private String slaType;

    @Column(name = "target_minutes", nullable = false)
    private int targetMinutes;

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public String getSlaType() {
        return slaType;
    }

    public void setSlaType(String slaType) {
        this.slaType = slaType;
    }

    public int getTargetMinutes() {
        return targetMinutes;
    }

    public void setTargetMinutes(int targetMinutes) {
        this.targetMinutes = targetMinutes;
    }
}
