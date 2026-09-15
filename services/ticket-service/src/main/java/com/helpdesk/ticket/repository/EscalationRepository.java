package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.domain.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscalationRepository extends JpaRepository<Escalation, UUID> {

    List<Escalation> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
