package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotifier implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TicketCreatedEvent;
    }

    @Override
    public void notify(DomainEvent event) {
        if (event instanceof TicketCreatedEvent ticketEvent) {
            log.info("Sending email for created ticket: {}", ticketEvent.ticketId());
            // Actual email dispatch logic goes here
        }
    }
}
