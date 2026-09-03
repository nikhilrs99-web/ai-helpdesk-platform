package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Subject/Dispatcher in the Observer pattern.
 * Manages the list of observers and notifies them when an event occurs.
 */
@Service
public class NotificationDispatcher {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private final List<NotificationObserver> observers;

    // Spring auto-injects all beans implementing NotificationObserver
    public NotificationDispatcher(List<NotificationObserver> observers) {
        this.observers = observers;
    }

    public void dispatch(DomainEvent event) {
        log.info("Dispatching event: {}", event.eventType());
        for (NotificationObserver observer : observers) {
            if (observer.supports(event)) {
                try {
                    observer.notify(event);
                } catch (Exception e) {
                    log.error("Observer {} failed to handle event {}", observer.getClass().getSimpleName(), event.eventId(), e);
                }
            }
        }
    }
}
