package com.helpdesk.ticket.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.BaseEntity;
import com.helpdesk.ticket.domain.Sla;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.outbox.OutboxEvent;
import com.helpdesk.ticket.outbox.OutboxRepository;
import com.helpdesk.ticket.repository.SlaRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit tests (mocked repositories, no Testcontainers) proving the job now actually
 * computes a breach from sla_targets + the ticket's own createdAt, rather than fabricating a
 * random ticket id every run.
 */
class SlaBreachJobTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Ticket ticketCreatedAt(Instant createdAt, TicketCategory category) throws Exception {
        Ticket ticket = new Ticket();
        ticket.setCategory(category);
        // status defaults to OPEN (a non-terminal status) on a fresh Ticket - no setter exists,
        // status changes only go through changeStatus's State-pattern validation.
        setInherited(ticket, "id", UUID.randomUUID());
        setInherited(ticket, "createdAt", createdAt);
        return ticket;
    }

    private void setInherited(Object target, String fieldName, Object value) throws Exception {
        Field field = BaseEntity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Sla slaTarget(TicketCategory category, int targetMinutes) {
        Sla sla = new Sla();
        sla.setCategory(category);
        sla.setSlaType("FIRST_RESPONSE");
        sla.setTargetMinutes(targetMinutes);
        return sla;
    }

    @Test
    void publishesBreachAndMarksTicketWhenPastDeadline() throws Exception {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        SlaRepository slaRepository = mock(SlaRepository.class);
        OutboxRepository outboxRepository = mock(OutboxRepository.class);

        // Deliberately well past the deadline (2h old vs. a 60-min target), not exactly at the
        // boundary - Instant.now()'s clock resolution can make an exact-boundary comparison
        // flaky (now vs. deadline landing on the same tick).
        Ticket ticket = ticketCreatedAt(Instant.now().minusSeconds(7200), TicketCategory.ACCESS);
        when(ticketRepository.findBySlaBreachNotifiedFalseAndStatusNotIn(any())).thenReturn(List.of(ticket));
        when(slaRepository.findByCategoryAndSlaType(TicketCategory.ACCESS, "FIRST_RESPONSE"))
                .thenReturn(Optional.of(slaTarget(TicketCategory.ACCESS, 60))); // 60 min target, well overdue

        new SlaBreachJob(ticketRepository, slaRepository, outboxRepository, objectMapper).detectSlaBreaches();

        assertThat(ticket.isSlaBreachNotified()).isTrue();
        verify(ticketRepository).save(ticket);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void doesNotPublishWhenStillWithinTarget() throws Exception {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        SlaRepository slaRepository = mock(SlaRepository.class);
        OutboxRepository outboxRepository = mock(OutboxRepository.class);

        Ticket ticket = ticketCreatedAt(Instant.now().minusSeconds(60), TicketCategory.ACCESS); // 1 min old
        when(ticketRepository.findBySlaBreachNotifiedFalseAndStatusNotIn(any())).thenReturn(List.of(ticket));
        when(slaRepository.findByCategoryAndSlaType(TicketCategory.ACCESS, "FIRST_RESPONSE"))
                .thenReturn(Optional.of(slaTarget(TicketCategory.ACCESS, 60))); // 60 min target, not yet due

        new SlaBreachJob(ticketRepository, slaRepository, outboxRepository, objectMapper).detectSlaBreaches();

        assertThat(ticket.isSlaBreachNotified()).isFalse();
        verify(ticketRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void skipsTicketsWithNoConfiguredSlaTarget() throws Exception {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        SlaRepository slaRepository = mock(SlaRepository.class);
        OutboxRepository outboxRepository = mock(OutboxRepository.class);

        Ticket ticket = ticketCreatedAt(Instant.now().minusSeconds(999_999), TicketCategory.FEATURE_REQUEST);
        when(ticketRepository.findBySlaBreachNotifiedFalseAndStatusNotIn(any())).thenReturn(List.of(ticket));
        when(slaRepository.findByCategoryAndSlaType(TicketCategory.FEATURE_REQUEST, "FIRST_RESPONSE"))
                .thenReturn(Optional.empty());

        new SlaBreachJob(ticketRepository, slaRepository, outboxRepository, objectMapper).detectSlaBreaches();

        verify(outboxRepository, never()).save(any());
    }
}
