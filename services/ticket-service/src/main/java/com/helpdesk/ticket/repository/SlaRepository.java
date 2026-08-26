package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.domain.Sla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SlaRepository extends JpaRepository<Sla, UUID> {
}
