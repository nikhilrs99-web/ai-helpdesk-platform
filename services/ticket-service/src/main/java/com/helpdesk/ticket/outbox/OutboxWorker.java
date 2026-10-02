package com.helpdesk.ticket.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final Duration ACK_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxWorker(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Deliberately not @Transactional at this level anymore: that used to wrap the whole
    // batch in one transaction, so a failure on event N rolled back events 1..N-1's
    // "published" flag even though those had *already* been acknowledged by the broker -
    // the DB would then disagree with reality, and the next poll would resend them,
    // duplicating a publish that already succeeded. Each event is now its own unit of work
    // (JpaRepository.save() is itself transactional), so one failure can only affect the
    // event that actually failed.
    @Scheduled(fixedDelayString = "${outbox.poll-interval:1000}")
    public void publishEvents() {
        List<OutboxEvent> pending = repository.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            // Key by aggregateId to ensure strict ordering per ticket. .get() blocks until the
            // broker actually acknowledges the write - ADR-0003 always claimed this happens
            // before marking a row published; previously it didn't (fire-and-forget).
            kafkaTemplate.send("ticket-events", event.getAggregateId(), event.getPayload())
                    .get(ACK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            event.markPublished();
            repository.save(event);
            log.info("Published event {} to Kafka topic ticket-events", event.getId());
        } catch (Exception e) {
            // Left unpublished on purpose - the next poll retries it. Consumers dedupe by
            // this event's own eventId (see TicketEventKafkaListener), so a retry that
            // actually succeeds after a false-negative timeout is safe to land twice.
            log.error("Failed to publish event {} to Kafka; will retry next poll", event.getId(), e);
        }
    }
}
