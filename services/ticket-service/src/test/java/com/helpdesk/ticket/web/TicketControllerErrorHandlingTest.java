package com.helpdesk.ticket.web;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.routing.RoutingStrategy;
import com.helpdesk.ticket.security.SecurityConfig;
import com.helpdesk.ticket.security.TicketSecurity;
import com.helpdesk.ticket.tickettype.BillingTicketHandler;
import com.helpdesk.ticket.tickettype.BugTicketHandler;
import com.helpdesk.ticket.tickettype.DefaultTicketTypeHandler;
import com.helpdesk.ticket.tickettype.TicketTypeHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every one of these HTTP-status mappings (400/404/409) has been proven manually with curl or
 * PowerShell on some earlier day, but none of them were ever pinned down as an automated test -
 * Day 13's TicketControllerSecurityTest covers 200/403 (authorization) but not
 * GlobalExceptionHandler's other branches. Without a test, a refactor could silently change one
 * of these mappings and nothing would fail.
 */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, TicketSecurity.class, TicketControllerErrorHandlingTest.RealTypeHandlerFactoryConfig.class})
class TicketControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private RoutingStrategy routingStrategy;

    // Stops OAuth2ResourceServerAutoConfiguration from resolving a real JwtDecoder against the
    // configured Keycloak issuer-uri during context startup - see TicketControllerSecurityTest.
    @MockBean
    private JwtDecoder jwtDecoder;

    // Real handlers, not mocked - the BUG-missing-metadata test needs BugTicketHandler's actual
    // validation to fire, not a stand-in that can't throw MissingTicketMetadataException.
    @TestConfiguration
    static class RealTypeHandlerFactoryConfig {
        @Bean
        TicketTypeHandlerFactory ticketTypeHandlerFactory() {
            return new TicketTypeHandlerFactory(
                    List.of(new BugTicketHandler(), new BillingTicketHandler(), new DefaultTicketTypeHandler()));
        }
    }

    private Ticket newOpenTicket() {
        Ticket ticket = new Ticket();
        ticket.setSubject("Can't log in");
        ticket.setDescription("Password reset link never arrives");
        ticket.setCategory(TicketCategory.ACCESS);
        ticket.setRequesterId("customer-1");
        return ticket;
    }

    @Test
    void getByIdReturns404WhenTicketDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tickets/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString(id.toString())));
    }

    @Test
    void updateReturns404WhenTicketDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/tickets/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Updated\",\"description\":\"Updated description\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatusReturns404WhenTicketDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/tickets/{id}/status", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_TRIAGED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatusReturns409WhenTheTransitionIsIllegal() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(newOpenTicket()));

        mockMvc.perform(patch("/api/tickets/{id}/status", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("OPEN")))
                .andExpect(jsonPath("$.message").value(containsString("RESOLVED")));
    }

    @Test
    void createReturns400WhenSubjectIsBlank() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"\",\"description\":\"Something is broken\",\"category\":\"ACCESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("subject")));
    }

    @Test
    void createReturns400WhenDescriptionExceedsMaxLength() throws Exception {
        String tooLong = "x".repeat(5001);

        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Valid subject\",\"description\":\"" + tooLong + "\",\"category\":\"ACCESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("description")));
    }

    @Test
    void createReturns400WithNamedMissingKeysWhenBugMetadataIsIncomplete() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"App crashes on save\",\"description\":\"Stack trace attached\","
                                + "\"category\":\"BUG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("BUG")))
                .andExpect(jsonPath("$.message").value(containsString("browser")))
                .andExpect(jsonPath("$.message").value(containsString("appVersion")));
    }

    @Test
    void updateReturns400WhenValidationFails() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/tickets/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"\",\"description\":\"Still a valid description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("subject")));
    }
}
