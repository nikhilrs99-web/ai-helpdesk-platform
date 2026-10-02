package com.helpdesk.ai.llm;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Circuit breaker around every LLM call. After enough failures in the sliding window the
 * breaker opens and calls fail fast with 503 instead of each request waiting on (and being
 * billed for) a provider that is already down or rate-limiting us.
 */
@Component
public class LlmGuard {

    private static final Logger log = LoggerFactory.getLogger(LlmGuard.class);

    private final CircuitBreaker breaker;

    public LlmGuard() {
        this(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }

    public LlmGuard(CircuitBreakerConfig config) {
        this.breaker = CircuitBreaker.of("llm", config);
    }

    public <T> T call(Supplier<T> llmCall) {
        try {
            return breaker.executeSupplier(llmCall);
        } catch (CallNotPermittedException e) {
            log.warn("LLM circuit breaker is open - failing fast");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is temporarily unavailable, please try again shortly.", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("LLM call failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The AI provider call failed.", e);
        }
    }

    public CircuitBreaker.State state() {
        return breaker.getState();
    }
}
