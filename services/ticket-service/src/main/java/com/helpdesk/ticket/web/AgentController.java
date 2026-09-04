package com.helpdesk.ticket.web;

import com.helpdesk.ticket.redis.AgentPresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentPresenceService presenceService;

    public AgentController(AgentPresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/ping")
    @PreAuthorize("hasAnyRole('agent','admin')")
    public ResponseEntity<Void> pingPresence(@AuthenticationPrincipal Jwt jwt) {
        presenceService.markAgentOnline(jwt.getSubject());
        return ResponseEntity.ok().build();
    }
}
