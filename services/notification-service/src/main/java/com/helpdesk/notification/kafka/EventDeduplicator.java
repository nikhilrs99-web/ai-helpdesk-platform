package com.helpdesk.notification.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Remembers which event ids have already been notified, in Redis (shared across replicas and
 * surviving restarts) with a TTL so the keyspace stays bounded. Kafka is at-least-once, so
 * the same event can be redelivered after a rebalance or restart.
 *
 * Fails open: if Redis is unreachable the event is treated as new. A rare duplicate email is
 * a better outcome than dropping (or endlessly retrying) a notification because a cache is down.
 */
@Component
public class EventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicator.class);
    static final Duration TTL = Duration.ofDays(7);
    static final String KEY_PREFIX = "notification:processed:";

    private final StringRedisTemplate redis;

    public EventDeduplicator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return true the first time this event id is seen, false for a duplicate */
    public boolean firstTime(String eventId) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + eventId, "1", TTL));
        } catch (RuntimeException e) {
            log.warn("Redis unavailable for de-duplication of event {}; treating as new", eventId, e);
            return true;
        }
    }
}
