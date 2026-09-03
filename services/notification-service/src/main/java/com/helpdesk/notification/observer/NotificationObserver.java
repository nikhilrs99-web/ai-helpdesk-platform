package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;

/**
 * Observer interface for the notification system.
 * Any class that wants to handle/send notifications for domain events implements this.
 */
public interface NotificationObserver {
    
    /**
     * Determine if this observer cares about the specific event.
     */
    boolean supports(DomainEvent event);

    /**
     * Handle the event (e.g., format and send an email or push notification).
     */
    void notify(DomainEvent event);
}
