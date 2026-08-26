package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.domain.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {
}
