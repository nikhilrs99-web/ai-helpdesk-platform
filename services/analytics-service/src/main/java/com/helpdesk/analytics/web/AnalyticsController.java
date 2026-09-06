package com.helpdesk.analytics.web;

import com.helpdesk.analytics.domain.TicketMetricRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final TicketMetricRepository repository;

    public AnalyticsController(TicketMetricRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardMetrics() {
        long totalTickets = repository.count();
        long breached = repository.countBreachedSlas();
        long autoResolved = repository.countAiAutoResolved();
        
        double slaCompliance = totalTickets == 0 ? 100.0 : ((double)(totalTickets - breached) / totalTickets) * 100;
        double aiResolutionRate = totalTickets == 0 ? 0.0 : ((double) autoResolved / totalTickets) * 100;

        return Map.of(
            "totalTickets", totalTickets,
            "slaCompliancePercentage", Math.round(slaCompliance * 100.0) / 100.0,
            "aiResolutionPercentage", Math.round(aiResolutionRate * 100.0) / 100.0
        );
    }
}
