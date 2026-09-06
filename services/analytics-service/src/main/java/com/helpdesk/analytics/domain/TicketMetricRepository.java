package com.helpdesk.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TicketMetricRepository extends JpaRepository<TicketMetric, UUID> {
    TicketMetric findByTicketId(UUID ticketId);

    @Query("SELECT COUNT(t) FROM TicketMetric t WHERE t.slaBreached = true")
    long countBreachedSlas();

    @Query("SELECT COUNT(t) FROM TicketMetric t WHERE t.aiAutoResolved = true")
    long countAiAutoResolved();
}
