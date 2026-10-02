package com.helpdesk.ai.llm;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmGuardTest {

    private final LlmGuard guard = new LlmGuard(CircuitBreakerConfig.custom()
            .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMinutes(1)).build());

    @Test
    void successfulCallPassesThrough() {
        assertThat(guard.call(() -> "ok")).isEqualTo("ok");
    }

    @Test
    void repeatedFailuresOpenTheCircuitAndFailFastWith503() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> guard.call(() -> { throw new IllegalStateException("provider down"); }))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        }

        assertThat(guard.state()).isEqualTo(CircuitBreaker.State.OPEN);
        boolean[] called = {false};
        assertThatThrownBy(() -> guard.call(() -> { called[0] = true; return "never"; }))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(called[0]).isFalse();
    }
}
