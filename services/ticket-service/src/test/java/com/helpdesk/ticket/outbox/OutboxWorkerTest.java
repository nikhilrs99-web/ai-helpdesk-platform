package com.helpdesk.ticket.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OutboxWorkerTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxWorker worker;

    @Test
    void shouldPublishPendingEvents() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "Ticket", UUID.randomUUID().toString(), "test.event", "{}");
        repository.save(event);

        worker.publishEvents();

        OutboxEvent updated = repository.findById(event.getId()).orElseThrow();
        assertThat(updated.isPublished()).isTrue();
    }
}
