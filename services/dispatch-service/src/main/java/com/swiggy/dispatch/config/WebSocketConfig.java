package com.swiggy.dispatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket and STOMP message broker configuration.
 *
 * <p>Clients connect to '/ws-tracking' and subscribe to '/topic/orders/{orderId}/tracking'
 * to receive live GPS telemetry and delivery progress.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint for test clients and modern browsers
        registry.addEndpoint("/ws-tracking")
                .setAllowedOriginPatterns("*");

        // SockJS fallback for legacy browsers or networks that intercept raw WebSocket frames
        registry.addEndpoint("/ws-tracking")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // DECISION: In-memory broker prefix for client subscriptions
        registry.enableSimpleBroker("/topic");

        // DECISION: Application prefix for messages routed to @MessageMapping controller handlers
        registry.setApplicationDestinationPrefixes("/app");
    }
}
