package com.tickets.infrastructure.adapter.in.worker.handler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tickets.infrastructure.adapter.in.worker.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handles {@code "order-created"} messages from SQS.
 *
 * <p>Parses the payload (orderId, eventId, quantity) and logs it.
 * This is a placeholder — replace the log with a use case call
 * when the business logic for order processing is defined.</p>
 */
@Component
@Profile("worker")
public class OrderCreatedHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedHandler.class);

    private final ObjectMapper objectMapper;

    public OrderCreatedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String messageType() {
        return "order-created";
    }

    @Override
    public Mono<Void> handle(String payload) {
        return Mono.fromRunnable(() -> {
            try {
                JsonNode json = objectMapper.readTree(payload);
                String orderId = json.get("orderId").asText();
                String eventId = json.get("eventId").asText();
                int quantity = json.get("quantity").asInt();

                log.info("Order created event received: orderId={}, eventId={}, quantity={}",
                        orderId, eventId, quantity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse order-created payload", e);
            }
        }).then();
    }
}
