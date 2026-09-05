package com.helpdesk.ai.tools;

import com.helpdesk.ai.tools.AgentToolsConfig.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AgentToolTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void testGetTicketStatusTool() {
        @SuppressWarnings("unchecked")
        Function<TicketRequest, String> tool = (Function<TicketRequest, String>) context.getBean("getTicketStatus");
        String result = tool.apply(new TicketRequest("TKT-999"));
        assertThat(result).contains("IN_PROGRESS");
    }

    @Test
    void testCreateEscalationTool_ReturnsPendingHumanApproval() {
        @SuppressWarnings("unchecked")
        Function<EscalationRequest, Map<String, Object>> tool = 
            (Function<EscalationRequest, Map<String, Object>>) context.getBean("createEscalation");
            
        Map<String, Object> result = tool.apply(new EscalationRequest("TKT-123", "Customer is very angry"));
        
        assertThat(result)
            .containsEntry("ticketId", "TKT-123")
            .containsEntry("status", "PENDING_HUMAN_APPROVAL");
    }
    
    @Test
    void testGetCustomerTicketsTool() {
        @SuppressWarnings("unchecked")
        Function<CustomerRequest, List<String>> tool = 
            (Function<CustomerRequest, List<String>>) context.getBean("getCustomerTickets");
            
        List<String> result = tool.apply(new CustomerRequest("CUST-555"));
        assertThat(result).hasSize(2);
    }
}
