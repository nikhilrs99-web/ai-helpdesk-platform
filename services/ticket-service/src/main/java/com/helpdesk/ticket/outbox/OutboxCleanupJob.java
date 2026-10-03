package com.helpdesk.ticket.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Deletes outbox rows that were already published to Kafka and are older than the retention
 * window. Without this the outbox table only ever grows. Unpublished rows are never touched,
 * however old - those are events still waiting to be delivered.
 */
@Component
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxRepository repository;
    private final Duration retention;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public OutboxCleanupJob(OutboxRepository repository,
                            @Value("${outbox.cleanup.retention-days:7}") long retentionDays) {
        this(repository, Duration.ofDays(retentionDays), Clock.systemUTC());
    }

    OutboxCleanupJob(OutboxRepository repository, Duration retention, Clock clock) {
        this.repository = repository;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${outbox.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        int deleted = repository.deletePublishedOlderThan(Instant.now(clock).minus(retention));
        log.info("Outbox cleanup: removed {} published event(s) older than {}", deleted, retention);
    }
}
