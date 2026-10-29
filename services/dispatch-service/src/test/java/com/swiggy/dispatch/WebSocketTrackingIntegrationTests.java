package com.swiggy.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.dispatch.dto.TrackingLocationPayload;
import com.swiggy.dispatch.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stage 4 Integration Tests verifying STOMP WebSockets and Redis Pub/Sub Backplane.
 *
 * <p>SEE: ADR-009 (Real-Time Location Streaming with STOMP & Redis Backplane)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WebSocketTrackingIntegrationTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        this.stompClient.setMessageConverter(converter);
    }

    @Test
    @DisplayName("Should broadcast rider GPS update through Redis Pub/Sub to subscribed STOMP WebSocket client")
    void shouldStreamTelemetryToSubscribedClient() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        String destination = "/topic/orders/" + orderId + "/tracking";
        String wsUrl = "ws://localhost:" + port + "/ws-tracking";

        BlockingQueue<TrackingLocationPayload> receivedFrames = new LinkedBlockingQueue<>();

        // 1. Establish persistent STOMP WebSocket connection
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        // 2. Subscribe to order tracking destination
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TrackingLocationPayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedFrames.offer((TrackingLocationPayload) payload);
            }
        });

        // Small pause for subscription registration across Spring broker
        Thread.sleep(500);

        // 3. Publish rider GPS update to Redis Pub/Sub backplane
        TrackingLocationPayload testUpdate = new TrackingLocationPayload(
                orderId,
                riderId,
                12.9352,
                77.6245,
                "EN_ROUTE_TO_DELIVERY",
                Instant.now()
        );
        trackingService.publishTrackingUpdate(testUpdate);

        // 4. Await STOMP frame delivery from Redis backplane to the WebSocket client
        TrackingLocationPayload delivered = receivedFrames.poll(5, TimeUnit.SECONDS);

        assertThat(delivered).isNotNull();
        assertThat(delivered.orderId()).isEqualTo(orderId);
        assertThat(delivered.riderId()).isEqualTo(riderId);
        assertThat(delivered.latitude()).isEqualTo(12.9352);
        assertThat(delivered.longitude()).isEqualTo(77.6245);
        assertThat(delivered.status()).isEqualTo("EN_ROUTE_TO_DELIVERY");

        session.disconnect();
    }
}
