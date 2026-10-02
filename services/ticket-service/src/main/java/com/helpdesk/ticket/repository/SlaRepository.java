package com.helpdesk.ticket.repository;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Sla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlaRepository extends JpaRepository<Sla, UUID> {

    Optional<Sla> findByCategoryAndSlaType(TicketCategory category, String slaType);
}
