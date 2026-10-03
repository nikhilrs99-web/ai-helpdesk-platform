package com.helpdesk.notification.observer;

import com.helpdesk.common.event.DomainEvent;
import com.helpdesk.common.event.EscalationApprovedEvent;
import com.helpdesk.common.event.SlaBreachedEvent;
import com.helpdesk.common.event.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends real email over SMTP (MailHog locally, any SMTP relay elsewhere - see spring.mail.*
 * and notification.email.* in application.yml).
 *
 * Recipients: the support mailbox always gets a copy; the requester additionally gets the
 * customer-facing events (ticket created, escalation approved) when the event carries their
 * address (taken from the JWT "email" claim at ticket creation). SLA breaches are internal,
 * so they go to support only.
 */
@Component
public class EmailNotifier implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String supportMailbox;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${notification.email.from}") String from,
                         @Value("${notification.email.to}") String supportMailbox) {
        this.mailSender = mailSender;
        this.from = from;
        this.supportMailbox = supportMailbox;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof TicketCreatedEvent
                || event instanceof SlaBreachedEvent
                || event instanceof EscalationApprovedEvent;
    }

    @Override
    public void notify(DomainEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        String requesterEmail = null;

        if (event instanceof TicketCreatedEvent e) {
            requesterEmail = e.requesterEmail();
            message.setSubject("[Helpdesk] New ticket " + e.ticketId());
            message.setText("A new " + e.category() + " ticket was created by " + e.requesterId()
                    + ".\nTicket: " + e.ticketId());
        } else if (event instanceof SlaBreachedEvent e) {
            message.setSubject("[Helpdesk] SLA breached for ticket " + e.ticketId());
            message.setText(e.slaType() + " SLA was breached at " + e.breachedAt()
                    + ".\nTicket: " + e.ticketId());
        } else if (event instanceof EscalationApprovedEvent e) {
            requesterEmail = e.requesterEmail();
            message.setSubject("[Helpdesk] Escalation approved for ticket " + e.ticketId());
            message.setText("Escalation " + e.escalationId() + " was approved by " + e.approvedBy()
                    + ".\nReason: " + e.reason() + "\nTicket: " + e.ticketId());
        } else {
            return;
        }

        if (requesterEmail != null && !requesterEmail.isBlank()) {
            message.setTo(requesterEmail);
            message.setBcc(supportMailbox);
        } else {
            message.setTo(supportMailbox);
        }

        mailSender.send(message);
        log.info("Sent '{}' email to {}", message.getSubject(), String.join(",", message.getTo()));
    }
}
