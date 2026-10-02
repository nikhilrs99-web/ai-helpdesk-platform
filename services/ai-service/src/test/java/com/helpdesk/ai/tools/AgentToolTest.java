package com.helpdesk.ai.tools;

import com.helpdesk.ai.tools.AgentToolsConfig.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Plain unit tests - no @SpringBootTest needed since AgentToolsConfig only depends on two
 * RestClient instances, which MockRestServiceServer can stand in for directly. Each test
 * also asserts the forwarded Authorization header, since that's the whole point of these
 * tools acting as the calling user rather than a separate service identity (see
 * AgentToolsConfig's class Javadoc).
 */
class AgentToolTest {

    private RestClient.Builder ticketBuilder;
    private RestClient.Builder kbBuilder;
    private MockRestServiceServer ticketServer;
    private MockRestServiceServer kbServer;
    private AgentToolsConfig tools;

    @BeforeEach
    void setUp() {
        ticketBuilder = RestClient.builder().baseUrl("http://ticket-service:8081");
        kbBuilder = RestClient.builder().baseUrl("http://kb-service:8082");
        ticketServer = MockRestServiceServer.bindTo(ticketBuilder).build();
        kbServer = MockRestServiceServer.bindTo(kbBuilder).build();
        tools = new AgentToolsConfig(ticketBuilder.build(), kbBuilder.build());

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "user-123")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTicketStatus_returnsRealStatus_andForwardsCallerToken() {
        ticketServer.expect(requestTo("http://ticket-service:8081/api/tickets/TKT-999"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"id\":\"TKT-999\",\"subject\":\"s\",\"status\":\"IN_PROGRESS\",\"category\":\"BUG\"}", MediaType.APPLICATION_JSON));

        Function<TicketRequest, String> tool = (Function<TicketRequest, String>) getBean("getTicketStatus");
        String result = tool.apply(new TicketRequest("TKT-999"));

        assertThat(result).contains("IN_PROGRESS");
        ticketServer.verify();
    }

    @Test
    void getTicketStatus_handles404Gracefully() {
        ticketServer.expect(requestTo("http://ticket-service:8081/api/tickets/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Function<TicketRequest, String> tool = (Function<TicketRequest, String>) getBean("getTicketStatus");
        String result = tool.apply(new TicketRequest("missing"));

        assertThat(result).containsIgnoringCase("not found");
    }

    @Test
    void searchKnowledgeBase_returnsRealArticles() {
        kbServer.expect(requestTo("http://kb-service:8082/api/articles/search?q=password&size=5"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"content\":[{\"title\":\"Password Reset\",\"body\":\"Click forgot password\"}]}", MediaType.APPLICATION_JSON));

        Function<SearchRequest, List<String>> tool = (Function<SearchRequest, List<String>>) getBean("searchKnowledgeBase");
        List<String> result = tool.apply(new SearchRequest("password"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("Password Reset");
    }

    @Test
    void getSLAStatus_reportsBreach() {
        String body = "{\"targetConfigured\":true,\"targetMinutes\":60,\"deadline\":\"" + Instant.now().minusSeconds(60) + "\",\"breached\":true,\"minutesRemaining\":-5}";
        ticketServer.expect(requestTo("http://ticket-service:8081/api/tickets/TKT-1/sla-status"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Function<TicketRequest, String> tool = (Function<TicketRequest, String>) getBean("getSLAStatus");
        String result = tool.apply(new TicketRequest("TKT-1"));

        assertThat(result).containsIgnoringCase("BREACHED");
    }

    @Test
    void getCustomerTickets_filtersByRequesterId() {
        ticketServer.expect(requestTo("http://ticket-service:8081/api/tickets?requesterId=cust-1&size=10&sort=createdAt,desc"))
                .andRespond(withSuccess("{\"content\":[{\"id\":\"TKT-1\",\"subject\":\"s\",\"status\":\"OPEN\",\"category\":\"BUG\"}]}", MediaType.APPLICATION_JSON));

        Function<CustomerRequest, List<String>> tool = (Function<CustomerRequest, List<String>>) getBean("getCustomerTickets");
        List<String> result = tool.apply(new CustomerRequest("cust-1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("TKT-1").contains("OPEN");
    }

    @Test
    void createEscalation_persistsAndReturnsPendingApproval() {
        ticketServer.expect(requestTo("http://ticket-service:8081/api/tickets/TKT-123/escalations"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"ESC-1\",\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        Function<EscalationRequest, Map<String, Object>> tool = (Function<EscalationRequest, Map<String, Object>>) getBean("createEscalation");
        Map<String, Object> result = tool.apply(new EscalationRequest("TKT-123", "Customer is very angry"));

        assertThat(result)
                .containsEntry("ticketId", "TKT-123")
                .containsEntry("status", "PENDING")
                .containsEntry("escalationId", "ESC-1");
    }

    @SuppressWarnings("unchecked")
    private Object getBean(String toolName) {
        return switch (toolName) {
            case "getTicketStatus" -> tools.getTicketStatus();
            case "searchKnowledgeBase" -> tools.searchKnowledgeBase();
            case "getSLAStatus" -> tools.getSLAStatus();
            case "getCustomerTickets" -> tools.getCustomerTickets();
            case "createEscalation" -> tools.createEscalation();
            default -> throw new IllegalArgumentException(toolName);
        };
    }
}
