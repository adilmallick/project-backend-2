package com.swiggy.dispatch.config;

import com.swiggy.dispatch.tracking.RedisTrackingSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Configuration registering Redis Pub/Sub backplane message listener.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@Configuration
public class RedisPubSubConfig {

    public static final String TRACKING_CHANNEL_PATTERN = "tracking:orders:*";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisTrackingSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(TRACKING_CHANNEL_PATTERN));
        return container;
    }
}
