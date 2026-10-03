package com.helpdesk.analytics.web;

import com.helpdesk.analytics.domain.TicketMetricRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.RequestParam;

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

    /** One point of the ticket-volume series: how many tickets were created on this UTC day. */
    public record VolumePoint(LocalDate date, long count) {}

    /**
     * Tickets created per UTC day for the last {@code days} days (1-90), oldest first, with
     * zero-ticket days included so the chart's x-axis has no gaps.
     */
    @GetMapping("/volume")
    public List<VolumePoint> getTicketVolume(@RequestParam(defaultValue = "7") int days) {
        int window = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = today.minusDays(window - 1L);

        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (int i = 0; i < window; i++) {
            perDay.put(first.plusDays(i), 0L);
        }
        Instant since = first.atStartOfDay().toInstant(ZoneOffset.UTC);
        for (var metric : repository.findByCreatedAtGreaterThanEqual(since)) {
            if (metric.getCreatedAt() == null) continue;
            perDay.computeIfPresent(metric.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), (d, c) -> c + 1);
        }

        List<VolumePoint> series = new ArrayList<>();
        perDay.forEach((d, c) -> series.add(new VolumePoint(d, c)));
        return series;
    }
}
