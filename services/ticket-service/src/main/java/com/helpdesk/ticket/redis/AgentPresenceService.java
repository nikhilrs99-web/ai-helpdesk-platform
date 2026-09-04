package com.helpdesk.ticket.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class AgentPresenceService {

    private final StringRedisTemplate redisTemplate;

    public AgentPresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markAgentOnline(String agentId) {
        String key = "presence:agent:" + agentId;
        // Expire after 2 minutes of no pings
        redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(2));
    }

    public boolean isAgentOnline(String agentId) {
        String key = "presence:agent:" + agentId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
