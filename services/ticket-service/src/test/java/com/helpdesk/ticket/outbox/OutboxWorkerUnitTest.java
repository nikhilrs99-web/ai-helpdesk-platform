package com.helpdesk.ticket.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mocked-KafkaTemplate unit tests, complementing the Testcontainers-based OutboxWorkerTest
 * (which needs a real broker). These pin down the two behaviors the audit specifically
 * flagged: waiting for the broker ack before marking published, and one event's failure not
 * affecting any other event in the same poll.
 */
class OutboxWorkerUnitTest {

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> mockKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    // A unique payload per event matters here, not just cosmetic - tests that stub send() by
    // matching on the payload need two events to actually be distinguishable, otherwise
    // Mockito's last-matching-stub-wins behavior makes both calls resolve to whichever stub
    // was registered last.
    private OutboxEvent newEvent() {
        String uniquePayload = "{\"marker\":\"" + UUID.randomUUID() + "\"}";
        return new OutboxEvent(UUID.randomUUID(), "Ticket", UUID.randomUUID().toString(), "test.event", uniquePayload);
    }

    @Test
    void marksPublishedOnlyAfterTheBrokerAcks() {
        OutboxRepository repository = mock(OutboxRepository.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();
        OutboxEvent event = newEvent();
        when(repository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        new OutboxWorker(repository, kafkaTemplate).publishEvents();

        assertThat(event.isPublished()).isTrue();
        verify(repository).save(event);
    }

    @Test
    void doesNotMarkPublishedWhenTheSendFails() {
        OutboxRepository repository = mock(OutboxRepository.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();
        OutboxEvent event = newEvent();
        when(repository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        new OutboxWorker(repository, kafkaTemplate).publishEvents();

        assertThat(event.isPublished()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void oneEventFailingDoesNotStopTheRestOfTheBatch() {
        OutboxRepository repository = mock(OutboxRepository.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();
        OutboxEvent failing = newEvent();
        OutboxEvent succeeding = newEvent();
        when(repository.findByPublishedFalseOrderByCreatedAtAsc()).thenReturn(List.of(failing, succeeding));

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(failing.getPayload())))
                .thenReturn(failedFuture);
        when(kafkaTemplate.send(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(succeeding.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        new OutboxWorker(repository, kafkaTemplate).publishEvents();

        assertThat(failing.isPublished()).isFalse();
        assertThat(succeeding.isPublished()).isTrue();
        verify(repository).save(succeeding);
        verify(repository, never()).save(failing);
    }
}
