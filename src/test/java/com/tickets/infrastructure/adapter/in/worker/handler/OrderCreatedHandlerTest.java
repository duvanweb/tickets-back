package com.tickets.infrastructure.adapter.in.worker.handler;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OrderCreatedHandlerTest {

    private OrderCreatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCreatedHandler(JsonMapper.builder().build());
    }

    @Test
    void messageType_returnsOrderCreated() {
        assert handler.messageType().equals("order-created");
    }

    @Test
    void handle_withValidPayload_completesSuccessfully() {
        String payload = """
                {"orderId":"ord-123","eventId":"evt-456","quantity":2}""";

        StepVerifier.create(handler.handle(payload))
                .verifyComplete();
    }

    @Test
    void handle_withInvalidPayload_propagatesError() {
        String payload = "not-valid-json";

        StepVerifier.create(handler.handle(payload))
                .verifyError(RuntimeException.class);
    }

    @Test
    void handle_withMissingFields_propagatesError() {
        String payload = """
                {"orderId":"ord-123"}""";

        StepVerifier.create(handler.handle(payload))
                .verifyError(RuntimeException.class);
    }
}
