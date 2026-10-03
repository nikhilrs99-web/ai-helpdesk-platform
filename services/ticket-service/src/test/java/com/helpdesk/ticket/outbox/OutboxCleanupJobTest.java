package com.helpdesk.ticket.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxCleanupJobTest {

    @Test
    void deletesOnlyPublishedRowsOlderThanTheRetentionWindow() {
        OutboxRepository repository = mock(OutboxRepository.class);
        Instant now = Instant.parse("2026-10-10T03:30:00Z");
        OutboxCleanupJob job = new OutboxCleanupJob(repository, Duration.ofDays(7),
                Clock.fixed(now, ZoneOffset.UTC));

        job.cleanup();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deletePublishedOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(Instant.parse("2026-10-03T03:30:00Z"));
    }
}
