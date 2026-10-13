package com.tickets.infrastructure.adapter.in.worker.handler;

import tools.jackson.databind.json.JsonMapper;
import com.tickets.application.port.in.order.ProcessOrderUseCase;
import com.tickets.application.port.in.order.ProcessOrderUseCase.ProcessOrderCommand;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedHandlerTest {

    @Mock
    private ProcessOrderUseCase processOrderUseCase;

    private OrderCreatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCreatedHandler(JsonMapper.builder().build(), processOrderUseCase);
    }

    @Test
    void messageType_returnsOrderCreated() {
        assert handler.messageType().equals("order-created");
    }

    @Test
    void handle_withValidPayload_invokesUseCaseAndCompletes() {
        OrderId orderId = OrderId.generate();
        EventId eventId = EventId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.RESERVED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10),
                List.of());

        when(processOrderUseCase.process(any(ProcessOrderCommand.class))).thenReturn(Mono.just(order));

        String payload = """
                {"orderId":"%s","eventId":"%s","customerId":"customer-123","quantity":2}"""
                .formatted(orderId.value().toString(), eventId.value().toString());

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

    @Test
    void handle_whenUseCaseErrors_propagatesError() {
        when(processOrderUseCase.process(any(ProcessOrderCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Processing failed")));

        String payload = """
                {"orderId":"550e8400-e29b-41d4-a716-446655440000","eventId":"550e8400-e29b-41d4-a716-446655440001","customerId":"cust-1","quantity":1}""";

        StepVerifier.create(handler.handle(payload))
                .verifyError(RuntimeException.class);
    }
}
