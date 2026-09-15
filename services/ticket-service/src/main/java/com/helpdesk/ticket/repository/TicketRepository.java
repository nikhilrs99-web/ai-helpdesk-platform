package com.helpdesk.ticket.repository;

import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    boolean existsByIdAndRequesterId(UUID id, String requesterId);

    Page<Ticket> findByRequesterId(String requesterId, Pageable pageable);

    // Candidates for SlaBreachJob: not already flagged, and not in a terminal status (a
    // RESOLVED/CLOSED ticket can't still be breaching its first-response target).
    List<Ticket> findBySlaBreachNotifiedFalseAndStatusNotIn(List<TicketStatus> terminalStatuses);
}
