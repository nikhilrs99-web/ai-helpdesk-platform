package com.helpdesk.notification.web;

import com.helpdesk.common.event.TicketCreatedEvent;
import com.helpdesk.notification.observer.NotificationDispatcher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationDispatcher dispatcher;

    public NotificationController(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // Temporary direct REST endpoint for Day 19 integration before Kafka replaces it in Phase 3
    @PostMapping("/ticket-created")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handleTicketCreated(@RequestBody TicketCreatedEvent event) {
        dispatcher.dispatch(event);
    }
}
