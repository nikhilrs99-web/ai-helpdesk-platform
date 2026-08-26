package com.helpdesk.ticket.domain;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.repository.AgentRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real, disposable Postgres container (not H2) so the CHECK constraints
 * Hibernate generates for the enum columns are actually exercised - a fake in-memory
 * database wouldn't catch a mismatch between Ticket's Java enum and the DB constraint.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TicketRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void savesTicketWithAuditingAndAgentRelationship() {
        Agent agent = new Agent();
        agent.setKeycloakSubjectId("keycloak-subject-abc");
        agent.setDisplayName("Test Agent");
        agent.setTeam("billing");
        agent = agentRepository.save(agent);

        Ticket ticket = new Ticket();
        ticket.setSubject("Double charged");
        ticket.setDescription("I was billed twice this month");
        ticket.setCategory(TicketCategory.BILLING);
        ticket.setRequesterId("keycloak-subject-customer-xyz");
        ticket.setAssignedAgent(agent);
        Ticket saved = ticketRepository.save(ticket);

        Ticket reloaded = ticketRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.getAssignedAgent().getId()).isEqualTo(agent.getId());
    }
}
