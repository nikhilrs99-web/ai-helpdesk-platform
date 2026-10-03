package com.helpdesk.notification.observer;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.EscalationApprovedEvent;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotifierTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailNotifier notifier = new EmailNotifier(mailSender, "noreply@helpdesk.local", "support@helpdesk.local");

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void ticketCreatedSendsRealEmail() {
        UUID ticketId = UUID.randomUUID();
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(),
                TicketCreatedEvent.CURRENT_VERSION, Instant.now(), ticketId,
                TicketCategory.ACCESS, "requester-1");

        assertThat(notifier.supports(event)).isTrue();
        notifier.notify(event);

        SimpleMailMessage msg = sentMessage();
        assertThat(msg.getTo()).containsExactly("support@helpdesk.local");
        assertThat(msg.getFrom()).isEqualTo("noreply@helpdesk.local");
        assertThat(msg.getSubject()).contains("New ticket").contains(ticketId.toString());
    }

    @Test
    void slaBreachedSendsRealEmail() {
        SlaBreachedEvent event = new SlaBreachedEvent(UUID.randomUUID(), SlaBreachedEvent.CURRENT_VERSION,
                Instant.now(), UUID.randomUUID(), "FIRST_RESPONSE", Instant.now());

        assertThat(notifier.supports(event)).isTrue();
        notifier.notify(event);

        assertThat(sentMessage().getSubject()).contains("SLA breached");
    }

    @Test
    void escalationApprovedSendsRealEmail() {
        EscalationApprovedEvent event = new EscalationApprovedEvent(UUID.randomUUID(),
                EscalationApprovedEvent.CURRENT_VERSION, Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), "Customer is very upset", "agent-1");

        assertThat(notifier.supports(event)).isTrue();
        notifier.notify(event);

        SimpleMailMessage msg = sentMessage();
        assertThat(msg.getSubject()).contains("Escalation approved");
        assertThat(msg.getText()).contains("agent-1").contains("Customer is very upset");
    }

    @Test
    void doesNotSupportOtherDomainEventTypes() {
        DomainEvent other = new DomainEvent() {
            public UUID eventId() { return UUID.randomUUID(); }
            public int version() { return 1; }
            public Instant occurredAt() { return Instant.now(); }
            public String eventType() { return "something.else"; }
        };

        assertThat(notifier.supports(other)).isFalse();
        notifier.notify(other);
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void ticketCreatedWithRequesterEmailGoesToRequesterWithSupportInBcc() {
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(),
                TicketCreatedEvent.CURRENT_VERSION, Instant.now(), UUID.randomUUID(),
                TicketCategory.ACCESS, "requester-1", "alice@example.com");

        notifier.notify(event);

        SimpleMailMessage msg = sentMessage();
        assertThat(msg.getTo()).containsExactly("alice@example.com");
        assertThat(msg.getBcc()).containsExactly("support@helpdesk.local");
    }

    @Test
    void slaBreachIsInternalEvenWhenRequesterEmailIsKnown() {
        SlaBreachedEvent event = new SlaBreachedEvent(UUID.randomUUID(), SlaBreachedEvent.CURRENT_VERSION,
                Instant.now(), UUID.randomUUID(), "FIRST_RESPONSE", Instant.now(), "alice@example.com");

        notifier.notify(event);

        assertThat(sentMessage().getTo()).containsExactly("support@helpdesk.local");
    }

    @Test
    void escalationApprovedReachesTheRequester() {
        EscalationApprovedEvent event = new EscalationApprovedEvent(UUID.randomUUID(),
                EscalationApprovedEvent.CURRENT_VERSION, Instant.now(), UUID.randomUUID(),
                UUID.randomUUID(), "Upset customer", "agent-1", "alice@example.com");

        notifier.notify(event);

        assertThat(sentMessage().getTo()).containsExactly("alice@example.com");
    }
}
