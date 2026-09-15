package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotifier implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TicketCreatedEvent || event instanceof SlaBreachedEvent;
    }

    // Still log-only - actually sending mail needs a decision on what to send to (most
    // likely a local dev SMTP catcher like MailHog), tracked separately.
    @Override
    public void notify(DomainEvent event) {
        if (event instanceof TicketCreatedEvent ticketEvent) {
            log.info("Sending email for created ticket: {}", ticketEvent.ticketId());
        } else if (event instanceof SlaBreachedEvent slaEvent) {
            log.info("Sending SLA-breach email for ticket: {} ({}, deadline was {})",
                    slaEvent.ticketId(), slaEvent.slaType(), slaEvent.breachedAt());
        }
    }
}
