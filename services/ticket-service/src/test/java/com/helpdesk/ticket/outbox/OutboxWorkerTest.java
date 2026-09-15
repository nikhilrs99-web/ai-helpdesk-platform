package com.helpdesk.ticket.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OutboxWorkerTest {

    // This is a full @SpringBootTest (not @DataJpaTest), so it boots ticket-service's entire
    // context - Flyway/JPA included - and without its own Postgres, that falls back to
    // application.yml's default (localhost:5433), which only exists if a docker-compose
    // Postgres happens to already be running on the host. Declaring one here, the same way
    // TicketRepositoryIT does, is what actually makes this test self-contained.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

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
