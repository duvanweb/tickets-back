package com.tickets.infrastructure.adapter.in.worker.handler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tickets.application.port.in.order.ProcessOrderUseCase;
import com.tickets.application.usecase.order.ProcessOrderCommand;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.OrderId;
import com.tickets.infrastructure.adapter.in.worker.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handles {@code "order-created"} messages from SQS.
 *
 * <p>Parses the payload (orderId, eventId, customerId, quantity) and invokes
 * {@link ProcessOrderUseCase} to reserve tickets for the order.</p>
 */
@Component
@Profile("worker")
public class OrderCreatedHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedHandler.class);

    private final ObjectMapper objectMapper;
    private final ProcessOrderUseCase processOrderUseCase;

    public OrderCreatedHandler(ObjectMapper objectMapper, ProcessOrderUseCase processOrderUseCase) {
        this.objectMapper = objectMapper;
        this.processOrderUseCase = processOrderUseCase;
    }

    @Override
    public String messageType() {
        return "order-created";
    }

    @Override
    public Mono<Void> handle(String payload) {
        return Mono.fromCallable(() -> parseCommand(payload))
                .flatMap(command -> processOrderUseCase.process(command)
                        .doOnNext(order -> log.info(
                                "Order processed: orderId={}, status={}",
                                order.id(), order.status())))
                .then();
    }

    private ProcessOrderCommand parseCommand(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            return new ProcessOrderCommand(
                    OrderId.of(json.get("orderId").asText()),
                    EventId.of(json.get("eventId").asText()),
                    json.get("customerId").asText(),
                    json.get("quantity").asInt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse order-created payload", e);
        }
    }
}
