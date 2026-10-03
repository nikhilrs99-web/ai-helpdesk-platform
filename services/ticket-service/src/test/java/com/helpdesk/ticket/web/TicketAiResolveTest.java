package com.helpdesk.ticket.web;

import tools.jackson.databind.ObjectMapper;
import com.helpdesk.common.enums.TicketStatus;
import com.helpdesk.ticket.domain.Escalation;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.domain.state.IllegalTicketTransitionException;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import com.helpdesk.ticket.repository.EscalationRepository;
import com.helpdesk.ticket.repository.SlaRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.web.dto.TicketResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketAiResolveTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final EscalationRepository escalationRepository = mock(EscalationRepository.class);
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final TicketController controller = new TicketController(ticketRepository, null, null,
            outboxRepository, tools.jackson.databind.json.JsonMapper.builder().build(), null,
            mock(SlaRepository.class), escalationRepository);

    private Ticket openTicket(UUID id) {
        Ticket t = new Ticket();
        org.springframework.test.util.ReflectionTestUtils.setField(t, "id", id);
        t.setSubject("s");
        t.setDescription("d");
        t.setRequesterId("u1");
        when(ticketRepository.findById(id)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
        return t;
    }

    @Test
    void aiResolveMarksTicketResolvedAndPublishesAiResolvedEvent() {
        UUID id = UUID.randomUUID();
        openTicket(id);
        when(escalationRepository.findByTicketIdOrderByCreatedAtDesc(id)).thenReturn(List.of());

        TicketResponse response = controller.aiResolve(id);

        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("\"aiResolved\":true").contains("ticket.updated");
    }

    @Test
    void aiResolveIsRejectedWhenAnEscalationExists() {
        UUID id = UUID.randomUUID();
        openTicket(id);
        when(escalationRepository.findByTicketIdOrderByCreatedAtDesc(id)).thenReturn(List.of(new Escalation()));

        assertThatThrownBy(() -> controller.aiResolve(id)).isInstanceOf(IllegalTicketTransitionException.class);
        verify(outboxRepository, never()).save(any());
    }
}
