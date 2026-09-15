package com.helpdesk.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Each tool forwards the *calling user's own* bearer token to ticket-service/kb-service
 * (see bearerToken() below), rather than acting under a separate elevated service
 * identity. That means a customer chatting with the agent only ever gets data their own
 * token would already let them see (ticket-service's existing @PreAuthorize/ownership
 * checks apply exactly as they would to a direct API call) - least-privilege by
 * construction, not by remembering to scope a service account correctly.
 *
 * This only works because Spring AI's tool-calling in AiController.agentChat runs
 * synchronously on the same request thread as the incoming HTTP call - no @Async, no
 * WebFlux hand-off - so SecurityContextHolder's ThreadLocal still holds the original
 * JwtAuthenticationToken when these tool bodies execute.
 */
@Configuration
public class AgentToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentToolsConfig.class);

    public record TicketRequest(String ticketId) {}
    public record CustomerRequest(String customerId) {}
    public record SearchRequest(String query) {}
    public record EscalationRequest(String ticketId, String reason) {}

    // Subsets of each service's real response DTOs - only the fields these tools actually
    // use. Jackson ignores the rest (fail-on-unknown-properties is off by default).
    private record TicketView(String id, String subject, String status, String category) {}
    private record PageView<T>(List<T> content) {}
    private record ArticleView(String title, String body) {}
    private record SlaStatusView(boolean targetConfigured, Integer targetMinutes, Instant deadline,
                                  boolean breached, Long minutesRemaining) {}
    private record EscalationView(String id, String status) {}

    private final RestClient ticketServiceClient;
    private final RestClient kbServiceClient;

    public AgentToolsConfig(@Qualifier("ticketServiceClient") RestClient ticketServiceClient,
                             @Qualifier("kbServiceClient") RestClient kbServiceClient) {
        this.ticketServiceClient = ticketServiceClient;
        this.kbServiceClient = kbServiceClient;
    }

    @Bean
    @Description("Get the current status of a support ticket")
    public Function<TicketRequest, String> getTicketStatus() {
        return request -> {
            log.info("Tool called: getTicketStatus for {}", request.ticketId());
            try {
                TicketView ticket = ticketServiceClient.get()
                        .uri("/api/tickets/{id}", request.ticketId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken())
                        .retrieve()
                        .body(TicketView.class);
                return ticket == null
                        ? "No ticket found with id " + request.ticketId()
                        : "Status for " + request.ticketId() + " is " + ticket.status();
            } catch (Exception e) {
                return describeError(e, "ticket " + request.ticketId());
            }
        };
    }

    @Bean
    @Description("Search the knowledge base for articles")
    public Function<SearchRequest, List<String>> searchKnowledgeBase() {
        return request -> {
            log.info("Tool called: searchKnowledgeBase for {}", request.query());
            try {
                PageView<ArticleView> page = kbServiceClient.get()
                        .uri(uri -> uri.path("/api/articles/search")
                                .queryParam("q", request.query())
                                .queryParam("size", 5)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken())
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<PageView<ArticleView>>() {});

                if (page == null || page.content().isEmpty()) {
                    return List.of("No knowledge base articles matched \"" + request.query() + "\".");
                }
                return page.content().stream()
                        .map(a -> "Article: " + a.title() + " - " + truncate(a.body(), 150))
                        .toList();
            } catch (Exception e) {
                return List.of(describeError(e, "knowledge base search"));
            }
        };
    }

    @Bean
    @Description("Get the SLA breach status and deadline for a ticket's first response")
    public Function<TicketRequest, String> getSLAStatus() {
        return request -> {
            log.info("Tool called: getSLAStatus for {}", request.ticketId());
            try {
                SlaStatusView sla = ticketServiceClient.get()
                        .uri("/api/tickets/{id}/sla-status", request.ticketId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken())
                        .retrieve()
                        .body(SlaStatusView.class);

                if (sla == null || !sla.targetConfigured()) {
                    return "No SLA target is configured for ticket " + request.ticketId() + "'s category.";
                }
                return "SLA for " + request.ticketId() + " (first response, " + sla.targetMinutes() + " min target): "
                        + (sla.breached()
                                ? "BREACHED (deadline was " + sla.deadline() + ")"
                                : "HEALTHY, " + sla.minutesRemaining() + " minutes remaining (deadline " + sla.deadline() + ")");
            } catch (Exception e) {
                return describeError(e, "SLA status for ticket " + request.ticketId());
            }
        };
    }

    @Bean
    @Description("Get a list of recent tickets opened by a customer (customerId is their Keycloak subject id)")
    public Function<CustomerRequest, List<String>> getCustomerTickets() {
        return request -> {
            log.info("Tool called: getCustomerTickets for {}", request.customerId());
            try {
                PageView<TicketView> page = ticketServiceClient.get()
                        .uri(uri -> uri.path("/api/tickets")
                                .queryParam("requesterId", request.customerId())
                                .queryParam("size", 10)
                                .queryParam("sort", "createdAt,desc")
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken())
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<PageView<TicketView>>() {});

                if (page == null || page.content().isEmpty()) {
                    return List.of("No tickets found for customer " + request.customerId());
                }
                return page.content().stream()
                        .map(t -> t.id() + " (" + t.status() + "): " + t.subject())
                        .toList();
            } catch (Exception e) {
                return List.of(describeError(e, "tickets for customer " + request.customerId()));
            }
        };
    }

    @Bean
    @Description("Draft an escalation request for a ticket. Creates a PENDING record that has no effect until a human agent approves it - always tell the user it still needs human approval.")
    public Function<EscalationRequest, Map<String, Object>> createEscalation() {
        return request -> {
            log.info("Tool called: createEscalation for {} due to {}", request.ticketId(), request.reason());
            try {
                EscalationView escalation = ticketServiceClient.post()
                        .uri("/api/tickets/{ticketId}/escalations", request.ticketId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken())
                        .body(Map.of("reason", request.reason()))
                        .retrieve()
                        .body(EscalationView.class);

                return Map.of(
                        "escalationId", escalation == null ? "unknown" : escalation.id(),
                        "ticketId", request.ticketId(),
                        "reason", request.reason(),
                        "status", escalation == null ? "UNKNOWN" : escalation.status(),
                        "nextStep", "Pending human approval - tell the user an agent still needs to approve this before anything happens."
                );
            } catch (Exception e) {
                return Map.of(
                        "ticketId", request.ticketId(),
                        "status", "FAILED",
                        "error", describeError(e, "escalation for ticket " + request.ticketId())
                );
            }
        };
    }

    private static String bearerToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("Agent tool invoked without an authenticated request context");
    }

    private String describeError(Exception e, String context) {
        if (e instanceof HttpClientErrorException.NotFound) {
            return context + " was not found.";
        }
        if (e instanceof HttpClientErrorException.Forbidden) {
            return "Not authorized to view " + context + ".";
        }
        if (e instanceof HttpClientErrorException.BadRequest) {
            return "Invalid request for " + context + ".";
        }
        if (e instanceof RestClientException) {
            log.warn("Agent tool call failed for {}", context, e);
            return context + " is temporarily unavailable.";
        }
        throw e instanceof RuntimeException re ? re : new RuntimeException(e);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
