package com.helpdesk.ticket.web;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.routing.RoutingStrategy;
import com.helpdesk.ticket.security.SecurityConfig;
import com.helpdesk.ticket.security.TicketSecurity;
import com.helpdesk.ticket.tickettype.TicketTypeHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the Day 13 authorization rules actually fire over real HTTP, not just that
 * TicketSecurity.isOwner returns the right boolean in isolation. TicketControllerRoutingTest
 * calls the controller directly, which bypasses @PreAuthorize entirely - it's AOP, so it
 * only intercepts calls made through a proxied bean inside a real Spring context, which is
 * exactly what @WebMvcTest + MockMvc gives us here.
 */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, TicketSecurity.class})
class TicketControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private RoutingStrategy routingStrategy;

    @MockBean
    private TicketTypeHandlerFactory typeHandlerFactory;

    // Stops OAuth2ResourceServerAutoConfiguration from resolving a real JwtDecoder against the
    // configured Keycloak issuer-uri during context startup - SecurityMockMvcRequestPostProcessors
    // .jwt() injects a pre-authenticated principal directly and never decodes a real token.
    @MockBean
    private JwtDecoder jwtDecoder;

    private Ticket ticketOwnedBy(String requesterId) {
        Ticket ticket = new Ticket();
        ticket.setSubject("Can't log in");
        ticket.setDescription("Password reset link never arrives");
        ticket.setCategory(TicketCategory.ACCESS);
        ticket.setRequesterId(requesterId);
        return ticket;
    }

    @Test
    void ownerCanReadTheirOwnTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticketOwnedBy("customer-1")));
        when(ticketRepository.existsByIdAndRequesterId(id, "customer-1")).thenReturn(true);

        mockMvc.perform(get("/api/tickets/{id}", id)
                        .with(jwt().jwt(j -> j.subject("customer-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterId").value("customer-1"));
    }

    @Test
    void nonOwnerCustomerIsForbiddenFromReadingSomeoneElsesTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.existsByIdAndRequesterId(id, "customer-2")).thenReturn(false);

        mockMvc.perform(get("/api/tickets/{id}", id)
                        .with(jwt().jwt(j -> j.subject("customer-2"))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void agentCanReadAnyonesTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticketOwnedBy("customer-1")));

        mockMvc.perform(get("/api/tickets/{id}", id)
                        .with(jwt().jwt(j -> j.subject("agent-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_agent"))))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotChangeStatusEvenOnTheirOwnTicket() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/tickets/{id}/status", id)
                        .with(jwt().jwt(j -> j.subject("customer-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_TRIAGED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanChangeStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket ticket = ticketOwnedBy("customer-1");
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/tickets/{id}/status", id)
                        .with(jwt().jwt(j -> j.subject("agent-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_agent")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AI_TRIAGED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void listIsScopedToOwnTicketsForACustomer() throws Exception {
        when(ticketRepository.findByRequesterId(eq("customer-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy("customer-1")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tickets")
                        .with(jwt().jwt(j -> j.subject("customer-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requesterId").value("customer-1"));
    }

    @Test
    void listReturnsEveryTicketForAnAgent() throws Exception {
        Page<Ticket> allTickets = new PageImpl<>(
                List.of(ticketOwnedBy("customer-1"), ticketOwnedBy("customer-2")), PageRequest.of(0, 20), 2);
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(allTickets);

        mockMvc.perform(get("/api/tickets")
                        .with(jwt().jwt(j -> j.subject("agent-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_agent"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
}
