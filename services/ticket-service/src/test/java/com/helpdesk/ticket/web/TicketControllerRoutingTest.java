package com.helpdesk.ticket.web;

import com.helpdesk.common.enums.TicketCategory;
import com.helpdesk.ticket.domain.Ticket;
import com.helpdesk.ticket.repository.TicketRepository;
import com.helpdesk.ticket.routing.RoutingStrategy;
import com.helpdesk.ticket.tickettype.BillingTicketHandler;
import com.helpdesk.ticket.tickettype.BugTicketHandler;
import com.helpdesk.ticket.tickettype.DefaultTicketTypeHandler;
import com.helpdesk.ticket.tickettype.TicketTypeHandlerFactory;
import com.helpdesk.ticket.web.dto.CreateTicketRequest;
import com.helpdesk.ticket.web.dto.TicketResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the Strategy pattern's actual payoff: swapping which RoutingStrategy is injected
 * changes ticket creation's behavior with zero changes to TicketController itself. The fake
 * strategy here returns a team ("quality-assurance") that CategoryBasedRoutingStrategy would
 * never produce for BUG - if this test passes, the controller genuinely delegates the
 * decision rather than hard-coding it.
 */
class TicketControllerRoutingTest {

    @Test
    void usesWhicheverRoutingStrategyIsInjected() {
        TicketRepository repository = mock(TicketRepository.class);
        when(repository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutingStrategy fakeStrategy = ticket -> "quality-assurance";
        TicketTypeHandlerFactory typeHandlerFactory = new TicketTypeHandlerFactory(
                List.of(new BugTicketHandler(), new BillingTicketHandler(), new DefaultTicketTypeHandler()));
        TicketController controller = new TicketController(repository, fakeStrategy, typeHandlerFactory);

        CreateTicketRequest request = new CreateTicketRequest(
                "Crashes on save", "Stack trace attached", TicketCategory.BUG,
                Map.of("browser", "Chrome", "appVersion", "1.2.3"));
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "test-subject-id")
                .build();

        ResponseEntity<TicketResponse> response = controller.create(request, jwt);

        assertThat(response.getBody().routedTeam()).isEqualTo("quality-assurance");
    }
}
