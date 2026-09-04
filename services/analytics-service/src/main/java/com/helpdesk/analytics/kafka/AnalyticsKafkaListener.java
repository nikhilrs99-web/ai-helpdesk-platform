package com.helpdesk.analytics.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsKafkaListener.class);

    @KafkaListener(topics = "ticket-events", groupId = "analytics-group")
    public void handleEvent(String payload) {
        log.info("Analytics service received raw event: {}", payload);
        // Skeleton logic for Day 30: Simply log the event.
        // In the future, this will extract metrics and save to a dimensional model.
    }
}
