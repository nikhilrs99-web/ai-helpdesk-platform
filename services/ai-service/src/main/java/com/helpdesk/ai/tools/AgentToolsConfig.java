package com.helpdesk.ai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class AgentToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentToolsConfig.class);

    public record TicketRequest(String ticketId) {}
    public record CustomerRequest(String customerId) {}
    public record SearchRequest(String query) {}
    public record EscalationRequest(String ticketId, String reason) {}

    @Bean
    @Description("Get the current status of a support ticket")
    public Function<TicketRequest, String> getTicketStatus() {
        return request -> {
            log.info("Tool called: getTicketStatus for {}", request.ticketId());
            // In a real app, this makes a REST/gRPC call to ticket-service or checks a DB replica
            return "Status for " + request.ticketId() + " is IN_PROGRESS";
        };
    }

    @Bean
    @Description("Search the knowledge base for articles")
    public Function<SearchRequest, List<String>> searchKnowledgeBase() {
        return request -> {
            log.info("Tool called: searchKnowledgeBase for {}", request.query());
            return List.of("Article: How to reset password", "Article: Login troubleshooting");
        };
    }

    @Bean
    @Description("Get the SLA breach status and deadline for a ticket")
    public Function<TicketRequest, String> getSLAStatus() {
        return request -> {
            log.info("Tool called: getSLAStatus for {}", request.ticketId());
            return "SLA for " + request.ticketId() + " expires in 4 hours. Status: HEALTHY";
        };
    }

    @Bean
    @Description("Get a list of recent tickets opened by a customer")
    public Function<CustomerRequest, List<String>> getCustomerTickets() {
        return request -> {
            log.info("Tool called: getCustomerTickets for {}", request.customerId());
            return List.of("TKT-101 (CLOSED)", "TKT-205 (OPEN)");
        };
    }

    @Bean
    @Description("Draft an escalation request for a ticket. Requires human approval before execution.")
    public Function<EscalationRequest, Map<String, Object>> createEscalation() {
        return request -> {
            log.info("Tool called: createEscalation for {} due to {}", request.ticketId(), request.reason());
            // Returns a draft object indicating it is pending human approval
            return Map.of(
                    "escalationId", "ESC-" + System.currentTimeMillis(),
                    "ticketId", request.ticketId(),
                    "reason", request.reason(),
                    "status", "PENDING_HUMAN_APPROVAL",
                    "nextStep", "Present this draft to the human agent for approval"
            );
        };
    }
}
