package com.helpdesk.ticket.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RateLimiterServiceTest {

    // Same gap as OutboxWorkerTest: a full @SpringBootTest boots Flyway/JPA too, and without
    // its own Postgres it falls back to application.yml's default (localhost:5433) - only
    // reachable if a docker-compose Postgres happens to already be running on the host.
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    @Autowired
    private RateLimiterService rateLimiterService;

    @Test
    void shouldEnforceRateLimit() {
        String testUser = "user-123";

        // First 5 requests should be allowed
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiterService.isAllowed(testUser)).isTrue();
        }

        // 6th request should be blocked
        assertThat(rateLimiterService.isAllowed(testUser)).isFalse();
    }
}
