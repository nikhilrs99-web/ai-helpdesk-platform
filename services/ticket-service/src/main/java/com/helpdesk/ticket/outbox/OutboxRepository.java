package com.helpdesk.ticket.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.createdAt < :cutoff")
    int deletePublishedOlderThan(@org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff);
}
