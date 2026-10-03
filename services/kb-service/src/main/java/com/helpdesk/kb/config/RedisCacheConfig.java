package com.helpdesk.kb.config;

import tools.jackson.databind.json.JsonMapper;
import com.helpdesk.kb.web.dto.ArticleResponse;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Without this, Spring's cache abstraction falls back to Java's built-in serialization for
 * cache values, which requires ArticleResponse to implement Serializable (it doesn't - real
 * error: "DefaultSerializer requires a Serializable payload") and, more importantly, is a
 * known deserialization-vulnerability surface (arbitrary gadget-chain exploits) if anything
 * with write access to this Redis instance were ever compromised.
 *
 * GenericJacksonJsonRedisSerializer looks like the obvious JSON alternative, but it embeds
 * the concrete Java class name in the JSON payload itself (a "@class" field) so it knows
 * what to deserialize back into - which just relocates the same class of deserialization
 * risk into JSON instead of avoiding it (confirmed: without also enabling default typing on
 * its ObjectMapper, deserialization silently returns a LinkedHashMap instead of
 * ArticleResponse, a ClassCastException at read time). Since this service only caches one
 * type (ArticleResponse, under the "articles" cache name), a type-specific
 * JacksonJsonRedisSerializer scoped to just that cache is both the fix and the safer
 * choice: the JSON never carries a class name the cache could be tricked into instantiating.
 */
@Configuration
@org.springframework.cache.annotation.EnableCaching // here, not on the application class, so test slices do not need a CacheManager
public class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer articlesCacheCustomizer() {
        JsonMapper mapper = JsonMapper.builder().build(); // Jackson 3: java.time support is built in
        JacksonJsonRedisSerializer<ArticleResponse> articleSerializer =
                new JacksonJsonRedisSerializer<>(mapper, ArticleResponse.class);

        RedisCacheConfiguration articlesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(articleSerializer));

        return builder -> builder.withCacheConfiguration("articles", articlesConfig);
    }
}
