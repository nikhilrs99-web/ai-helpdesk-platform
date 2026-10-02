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
 * Sends a real email over SMTP (MailHog locally, any SMTP relay elsewhere - see
 * spring.mail.* and notification.email.* in application.yml). Events carry only ids, not
 * addresses, so every message goes to one configured support mailbox rather than to the
 * individual requester; resolving per-user addresses needs a user-lookup call and is a
 * separate change.
 */
@Component
public class EmailNotifier implements NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String to;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${notification.email.from}") String from,
                         @Value("${notification.email.to}") String to) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
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
        message.setTo(to);

        if (event instanceof TicketCreatedEvent e) {
            message.setSubject("[Helpdesk] New ticket " + e.ticketId());
            message.setText("A new " + e.category() + " ticket was created by " + e.requesterId()
                    + ".\nTicket: " + e.ticketId());
        } else if (event instanceof SlaBreachedEvent e) {
            message.setSubject("[Helpdesk] SLA breached for ticket " + e.ticketId());
            message.setText(e.slaType() + " SLA was breached at " + e.breachedAt()
                    + ".\nTicket: " + e.ticketId());
        } else if (event instanceof EscalationApprovedEvent e) {
            message.setSubject("[Helpdesk] Escalation approved for ticket " + e.ticketId());
            message.setText("Escalation " + e.escalationId() + " was approved by " + e.approvedBy()
                    + ".\nReason: " + e.reason() + "\nTicket: " + e.ticketId());
        } else {
            return;
        }

        mailSender.send(message);
        log.info("Sent '{}' email to {}", message.getSubject(), to);
    }
}
