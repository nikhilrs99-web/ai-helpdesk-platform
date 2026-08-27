package com.helpdesk.ticket.security;

import com.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketSecurityTest {

    @Test
    void isOwnerIsTrueWhenTheTokenSubjectMatchesTheTicketsRequesterId() {
        TicketRepository repository = mock(TicketRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.existsByIdAndRequesterId(ticketId, "customer-1")).thenReturn(true);
        TicketSecurity ticketSecurity = new TicketSecurity(repository);

        Authentication authentication = new TestingAuthenticationToken("customer-1", null);

        assertThat(ticketSecurity.isOwner(authentication, ticketId)).isTrue();
    }

    @Test
    void isOwnerIsFalseForAnUnrelatedCaller() {
        TicketRepository repository = mock(TicketRepository.class);
        UUID ticketId = UUID.randomUUID();
        when(repository.existsByIdAndRequesterId(ticketId, "customer-2")).thenReturn(false);
        TicketSecurity ticketSecurity = new TicketSecurity(repository);

        Authentication authentication = new TestingAuthenticationToken("customer-2", null);

        assertThat(ticketSecurity.isOwner(authentication, ticketId)).isFalse();
    }
}
