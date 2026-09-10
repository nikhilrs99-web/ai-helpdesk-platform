package com.helpdesk.analytics.web;

import com.helpdesk.analytics.domain.TicketMetricRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The only logic worth pinning down here is the percentage math in getDashboardMetrics(),
 * including the zero-tickets edge case (would otherwise divide by zero) and that rounding
 * matches what the frontend dashboard expects (2 decimal places).
 */
@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketMetricRepository repository;

    @Test
    void zeroTicketsReportsFullComplianceAndNoAiResolution() throws Exception {
        when(repository.count()).thenReturn(0L);
        when(repository.countBreachedSlas()).thenReturn(0L);
        when(repository.countAiAutoResolved()).thenReturn(0L);

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(0))
                .andExpect(jsonPath("$.slaCompliancePercentage").value(100.0))
                .andExpect(jsonPath("$.aiResolutionPercentage").value(0.0));
    }

    @Test
    void computesRoundedPercentagesFromRepositoryCounts() throws Exception {
        // 3 breached out of 7 -> (4/7)*100 = 57.142857... -> rounds to 57.14
        // 2 auto-resolved out of 7 -> 28.571428... -> rounds to 28.57
        when(repository.count()).thenReturn(7L);
        when(repository.countBreachedSlas()).thenReturn(3L);
        when(repository.countAiAutoResolved()).thenReturn(2L);

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(7))
                .andExpect(jsonPath("$.slaCompliancePercentage").value(57.14))
                .andExpect(jsonPath("$.aiResolutionPercentage").value(28.57));
    }

    @Test
    void allTicketsBreachedReportsZeroCompliance() throws Exception {
        when(repository.count()).thenReturn(5L);
        when(repository.countBreachedSlas()).thenReturn(5L);
        when(repository.countAiAutoResolved()).thenReturn(0L);

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaCompliancePercentage").value(0.0));
    }
}
