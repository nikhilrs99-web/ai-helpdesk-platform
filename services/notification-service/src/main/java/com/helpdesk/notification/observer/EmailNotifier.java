package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.EscalationApprovedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotifier implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TicketCreatedEvent || event instanceof EscalationApprovedEvent;
    }

    // Still log-only - actually sending mail needs a decision on what to send to (most
    // likely a local dev SMTP catcher like MailHog), tracked separately.
    @Override
    public void notify(DomainEvent event) {
        if (event instanceof TicketCreatedEvent ticketEvent) {
            log.info("Sending email for created ticket: {}", ticketEvent.ticketId());
        } else if (event instanceof EscalationApprovedEvent escalationEvent) {
            log.info("Sending escalation-approved email for ticket: {} (escalation {}, approved by {})",
                    escalationEvent.ticketId(), escalationEvent.escalationId(), escalationEvent.approvedBy());
        }
    }
}
