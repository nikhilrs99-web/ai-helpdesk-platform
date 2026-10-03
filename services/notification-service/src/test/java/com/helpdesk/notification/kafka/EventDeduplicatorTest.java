package com.helpdesk.notification.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventDeduplicatorTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final EventDeduplicator deduplicator = new EventDeduplicator(redis);

    EventDeduplicatorTest() {
        when(redis.opsForValue()).thenReturn(ops);
    }

    @Test
    void firstSightingIsNewAndIsStoredWithATtl() {
        when(ops.setIfAbsent(anyString(), eq("1"), eq(EventDeduplicator.TTL))).thenReturn(true);

        assertThat(deduplicator.firstTime("evt-1")).isTrue();
        verify(ops).setIfAbsent(EventDeduplicator.KEY_PREFIX + "evt-1", "1", EventDeduplicator.TTL);
    }

    @Test
    void secondSightingIsADuplicate() {
        when(ops.setIfAbsent(anyString(), anyString(), eq(EventDeduplicator.TTL))).thenReturn(false);

        assertThat(deduplicator.firstTime("evt-1")).isFalse();
    }

    @Test
    void failsOpenWhenRedisIsDown() {
        when(ops.setIfAbsent(anyString(), anyString(), eq(EventDeduplicator.TTL)))
                .thenThrow(new IllegalStateException("redis down"));

        assertThat(deduplicator.firstTime("evt-1")).isTrue();
    }
}
