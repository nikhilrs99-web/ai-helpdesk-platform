package com.helpdesk.ticket.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Throwaway endpoint to prove the OAuth2 resource server config actually works before any
 * real domain endpoint exists. Replaced once Day 10's ticket endpoints land.
 */
@RestController
public class WhoAmIController {

    @GetMapping("/api/tickets/whoami")
    @SuppressWarnings("unchecked")
    public Map<String, Object> whoAmI(@AuthenticationPrincipal Jwt jwt, Principal principal) {
        List<String> roles = jwt.getClaimAsMap("realm_access") == null
                ? List.of()
                : (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
        return Map.of(
                "username", principal.getName(),
                "roles", roles
        );
    }
}
