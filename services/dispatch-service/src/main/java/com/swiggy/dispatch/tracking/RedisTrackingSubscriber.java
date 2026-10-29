package com.swiggy.dispatch.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.dispatch.dto.TrackingLocationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub Subscriber listening to distributed tracking updates across the cluster.
 *
 * <p>DECISION: Acting as the distributed backplane, this subscriber receives messages published by
 * any dispatch-service instance and forwards them to locally connected WebSocket clients via STOMP.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTrackingSubscriber implements MessageListener {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            TrackingLocationPayload payload = objectMapper.readValue(body, TrackingLocationPayload.class);
            String destination = "/topic/orders/" + payload.orderId() + "/tracking";

            log.debug("Redis Pub/Sub received for channel='{}'. Forwarding to STOMP destination='{}'",
                    channel, destination);

            simpMessagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.error("Failed to deserialize and forward tracking message: {}", ex.getMessage(), ex);
        }
    }
}
