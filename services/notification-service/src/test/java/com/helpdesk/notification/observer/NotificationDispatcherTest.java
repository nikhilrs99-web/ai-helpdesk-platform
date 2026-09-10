package com.helpdesk.notification.observer;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Observer pattern's whole payoff is that dispatch() doesn't know or care what the
 * observers do - it just needs to route to the ones that opt in via supports(), and not let
 * one observer's failure take down the others.
 */
class NotificationDispatcherTest {

    private TicketCreatedEvent someEvent() {
        return new TicketCreatedEvent(UUID.randomUUID(), TicketCreatedEvent.CURRENT_VERSION,
                Instant.now(), UUID.randomUUID(), TicketCategory.ACCESS, "requester-1");
    }

    @Test
    void onlyNotifiesObserversThatSupportTheEvent() {
        NotificationObserver interested = mock(NotificationObserver.class);
        NotificationObserver uninterested = mock(NotificationObserver.class);
        TicketCreatedEvent event = someEvent();
        when(interested.supports(event)).thenReturn(true);
        when(uninterested.supports(event)).thenReturn(false);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(interested, uninterested));

        dispatcher.dispatch(event);

        verify(interested).notify(event);
        verify(uninterested, never()).notify(event);
    }

    @Test
    void oneObserverThrowingDoesNotStopTheOthersFromRunning() {
        NotificationObserver failing = mock(NotificationObserver.class);
        NotificationObserver healthy = mock(NotificationObserver.class);
        TicketCreatedEvent event = someEvent();
        when(failing.supports(event)).thenReturn(true);
        when(healthy.supports(event)).thenReturn(true);
        doThrow(new RuntimeException("SMTP is down")).when(failing).notify(event);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(failing, healthy));

        // Must not throw - a broken observer should never crash the Kafka listener thread
        // that ultimately calls this.
        dispatcher.dispatch(event);

        verify(healthy).notify(event);
    }

    @Test
    void noObserversSupportingTheEventDispatchesToNone() {
        NotificationObserver uninterested = mock(NotificationObserver.class);
        DomainEvent event = someEvent();
        when(uninterested.supports(event)).thenReturn(false);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(uninterested));

        dispatcher.dispatch(event);

        verify(uninterested, never()).notify(event);
    }
}
