package com.helpdesk.ticket.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    
    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxWorker(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:1000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> pending = repository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            log.info("Publishing event {} to Kafka topic ticket-events", event.getId());
            // Key by aggregateId to ensure strict ordering per ticket
            kafkaTemplate.send("ticket-events", event.getAggregateId(), event.getPayload());
            event.markPublished();
            repository.save(event);
        }
    }
}
