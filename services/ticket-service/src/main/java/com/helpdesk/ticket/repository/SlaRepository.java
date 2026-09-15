package com.helpdesk.ticket.repository;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Sla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlaRepository extends JpaRepository<Sla, UUID> {

    // Also added independently on the not-yet-merged fix/ai-service-agent-tools branch
    // (for GET /api/tickets/{id}/sla-status) - identical method, trivial merge conflict to
    // resolve whichever PR lands second.
    Optional<Sla> findByCategoryAndSlaType(TicketCategory category, String slaType);
}
