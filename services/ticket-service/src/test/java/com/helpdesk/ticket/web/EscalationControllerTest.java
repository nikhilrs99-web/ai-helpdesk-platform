package com.helpdesk.ticket.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.helpdesk.ticket.domain.Escalation;
import com.helpdesk.ticket.domain.EscalationStatus;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import com.helpdesk.ticket.repository.EscalationRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test (mocked repositories, no MockMvc/Spring context needed) proving approve()
 * actually publishes escalation.approved through the outbox - the "triggers notifications"
 * half of docs/rag/agent-tools.md's workflow that didn't exist before this change.
 */
class EscalationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Escalation pendingEscalation(UUID ticketId) {
        Escalation escalation = new Escalation();
        escalation.setTicketId(ticketId);
        escalation.setReason("Customer is very upset");
        escalation.setRequestedBy("customer-1");
        return escalation;
    }

    @Test
    void approvePublishesEscalationApprovedEventThroughTheOutbox() {
        EscalationRepository escalationRepository = mock(EscalationRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        OutboxRepository outboxRepository = mock(OutboxRepository.class);

        UUID ticketId = UUID.randomUUID();
        UUID escalationId = UUID.randomUUID();
        Escalation escalation = pendingEscalation(ticketId);
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(escalation));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "agent-1")
                .build();

        EscalationController controller = new EscalationController(
                escalationRepository, ticketRepository, outboxRepository, objectMapper);
        controller.approve(ticketId, escalationId, jwt);

        assertThat(escalation.getStatus()).isEqualTo(EscalationStatus.APPROVED);
        assertThat(escalation.getDecidedBy()).isEqualTo("agent-1");

        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void approvedEventPayloadContainsTheTicketAndEscalationIds() {
        EscalationRepository escalationRepository = mock(EscalationRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        OutboxRepository outboxRepository = mock(OutboxRepository.class);

        UUID ticketId = UUID.randomUUID();
        UUID escalationId = UUID.randomUUID();
        Escalation escalation = pendingEscalation(ticketId);
        when(escalationRepository.findById(escalationId)).thenReturn(Optional.of(escalation));
        when(escalationRepository.save(any(Escalation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "agent-1")
                .build();

        EscalationController controller = new EscalationController(
                escalationRepository, ticketRepository, outboxRepository, objectMapper);
        controller.approve(ticketId, escalationId, jwt);

        org.mockito.ArgumentCaptor<OutboxEvent> captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent published = captor.getValue();

        assertThat(published.getEventType()).isEqualTo("escalation.approved");
        assertThat(published.getAggregateId()).isEqualTo(ticketId.toString());
        assertThat(published.getPayload()).contains(ticketId.toString()).contains("agent-1");
    }
}
