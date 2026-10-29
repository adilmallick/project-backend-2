package com.swiggy.catalog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Production Redis Cache configuration for Catalog Service.
 *
 * <p>DECISION: Use Jackson JSON serialization rather than Java Native Serialization (JdkSerializationRedisSerializer)
 * to avoid Java security deserialization exploits and allow inspecting cache items in redis-cli as human-readable JSON.
 *
 * <p>DECISION: Implement resilient CacheErrorHandler. If Redis node crashes or experiences network partition,
 * the cache lookup fails gracefully with a WARN log and allows the service to fall back directly to PostgreSQL.
 *
 * <p>SEE: ADR-006 (Redis Caching & Invalidation Strategies)
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    public static final String RESTAURANT_CACHE = "restaurants";
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration(ObjectMapper baseObjectMapper) {
        ObjectMapper cacheMapper = baseObjectMapper.copy();
        cacheMapper.registerModule(new JavaTimeModule());
        cacheMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaultCacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(RESTAURANT_CACHE, defaultCacheConfiguration.entryTtl(Duration.ofMinutes(15)));
    }

    /**
     * Resilient error handler for Redis outages (Stage 2 Chaos requirement).
     * When Redis connection drops, log a warning and let the application fetch data from PostgreSQL.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis unavailable during cache GET for key='{}' in cache='{}'. Falling back to PostgreSQL DB. Error: {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis unavailable during cache PUT for key='{}' in cache='{}'. Error: {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis unavailable during cache EVICT for key='{}' in cache='{}'. Error: {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis unavailable during cache CLEAR in cache='{}'. Error: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
