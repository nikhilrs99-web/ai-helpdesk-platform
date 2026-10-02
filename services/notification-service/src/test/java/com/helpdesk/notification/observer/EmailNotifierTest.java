package com.helpdesk.notification.observer;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EmailNotifierTest {

    private final EmailNotifier notifier = new EmailNotifier();

    @Test
    void supportsTicketCreatedEvents() {
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(),
                TicketCreatedEvent.CURRENT_VERSION, Instant.now(), UUID.randomUUID(),
                TicketCategory.ACCESS, "requester-1");

        assertThat(notifier.supports(event)).isTrue();
    }

    @Test
    void supportsSlaBreachedEvents() {
        SlaBreachedEvent event = new SlaBreachedEvent(UUID.randomUUID(), SlaBreachedEvent.CURRENT_VERSION,
                Instant.now(), UUID.randomUUID(), "FIRST_RESPONSE", Instant.now());

        assertThat(notifier.supports(event)).isTrue();
        assertThatCode(() -> notifier.notify(event)).doesNotThrowAnyException();
    }

    @Test
    void doesNotSupportOtherDomainEventTypes() {
        DomainEvent other = new DomainEvent() {
            public UUID eventId() { return UUID.randomUUID(); }
            public int version() { return 1; }
            public Instant occurredAt() { return Instant.now(); }
            public String eventType() { return "sla.breached"; }
        };

        assertThat(notifier.supports(other)).isFalse();
    }

    @Test
    void notifyingWithASupportedEventDoesNotThrow() {
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(),
                TicketCreatedEvent.CURRENT_VERSION, Instant.now(), UUID.randomUUID(),
                TicketCategory.ACCESS, "requester-1");

        assertThatCode(() -> notifier.notify(event)).doesNotThrowAnyException();
    }
}
