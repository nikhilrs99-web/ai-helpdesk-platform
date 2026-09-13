package com.helpdesk.kb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.helpdesk.kb.web.dto.ArticleResponse;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Without this, Spring's cache abstraction falls back to Java's built-in serialization for
 * cache values, which requires ArticleResponse to implement Serializable (it doesn't - real
 * error: "DefaultSerializer requires a Serializable payload") and, more importantly, is a
 * known deserialization-vulnerability surface (arbitrary gadget-chain exploits) if anything
 * with write access to this Redis instance were ever compromised.
 *
 * GenericJackson2JsonRedisSerializer looks like the obvious JSON alternative, but it embeds
 * the concrete Java class name in the JSON payload itself (a "@class" field) so it knows
 * what to deserialize back into - which just relocates the same class of deserialization
 * risk into JSON instead of avoiding it (confirmed: without also enabling default typing on
 * its ObjectMapper, deserialization silently returns a LinkedHashMap instead of
 * ArticleResponse, a ClassCastException at read time). Since this service only caches one
 * type (ArticleResponse, under the "articles" cache name), a type-specific
 * Jackson2JsonRedisSerializer scoped to just that cache is both the fix and the safer
 * choice: the JSON never carries a class name the cache could be tricked into instantiating.
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer articlesCacheCustomizer() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<ArticleResponse> articleSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, ArticleResponse.class);

        RedisCacheConfiguration articlesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(articleSerializer));

        return builder -> builder.withCacheConfiguration("articles", articlesConfig);
    }
}
