package com.tickets.infrastructure.adapter.in.web;

import com.tickets.application.port.in.order.ConfirmOrderUseCase;
import com.tickets.application.port.in.order.CreateOrderUseCase;
import com.tickets.application.port.in.order.GetOrderUseCase;
import com.tickets.application.usecase.order.CreateOrderCommand;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    @Mock
    private ConfirmOrderUseCase confirmOrderUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(createOrderUseCase, getOrderUseCase, confirmOrderUseCase);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_BODY = """
            {"eventId": "550e8400-e29b-41d4-a716-446655440000", "customerId": "customer-123", "quantity": 3}""";

    @Test
    void create_withValidBody_returns202() {
        Order order = Order.create(EventId.generate(), "customer-123", 3);
        when(createOrderUseCase.create(any(CreateOrderCommand.class))).thenReturn(Mono.just(order));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("customer-123")
                .jsonPath("$.quantity").isEqualTo(3)
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    void create_withBlankEventId_returns400() {
        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId": "", "customerId": "customer-123", "quantity": 3}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withBlankCustomerId_returns400() {
        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId": "550e8400-e29b-41d4-a716-446655440000", "customerId": "", "quantity": 3}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withZeroQuantity_returns400() {
        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"eventId": "550e8400-e29b-41d4-a716-446655440000", "customerId": "customer-123", "quantity": 0}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_whenInvalidOrder_returns400() {
        when(createOrderUseCase.create(any(CreateOrderCommand.class)))
                .thenReturn(Mono.error(new InvalidOrderException("Quantity must be positive")));

        webTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Quantity must be positive");
    }

    @Test
    void getById_whenExists_returns200() {
        OrderId id = OrderId.generate();
        Order order = Order.create(EventId.generate(), "customer-123", 3);
        Order reconstituted = Order.reconstitute(id, order.eventId(), order.customerId(),
                order.quantity(), order.status(), order.createdAt(), order.reservedUntil(), order.ticketIds());
        when(getOrderUseCase.getById(id)).thenReturn(Mono.just(reconstituted));

        webTestClient.get().uri("/api/orders/{id}", id.value().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.value().toString())
                .jsonPath("$.customerId").isEqualTo("customer-123")
                .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void getById_whenNotFound_returns404() {
        OrderId id = OrderId.generate();
        when(getOrderUseCase.getById(id)).thenReturn(Mono.error(new OrderNotFoundException("Order not found")));

        webTestClient.get().uri("/api/orders/{id}", id.value().toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Order not found");
    }

    @Test
    void confirm_withValidOrder_returns200WithSoldStatus() {
        OrderId id = OrderId.generate();
        EventId eventId = EventId.generate();
        Order soldOrder = Order.reconstitute(id, eventId, "customer-123", 2,
                OrderStatus.SOLD, java.time.LocalDateTime.now().minusMinutes(5),
                java.time.LocalDateTime.now().plusMinutes(5),
                List.of(com.tickets.domain.model.ticket.TicketId.generate(),
                        com.tickets.domain.model.ticket.TicketId.generate()));
        when(confirmOrderUseCase.confirm(id)).thenReturn(Mono.just(soldOrder));

        webTestClient.post().uri("/api/orders/{id}/confirm", id.value().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.value().toString())
                .jsonPath("$.status").isEqualTo("SOLD");
    }

    @Test
    void confirm_whenOrderNotFound_returns404() {
        OrderId id = OrderId.generate();
        when(confirmOrderUseCase.confirm(id)).thenReturn(Mono.error(new OrderNotFoundException("Order not found")));

        webTestClient.post().uri("/api/orders/{id}/confirm", id.value().toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Order not found");
    }

    @Test
    void confirm_whenOrderNotReserved_returns400() {
        OrderId id = OrderId.generate();
        when(confirmOrderUseCase.confirm(id)).thenReturn(Mono.error(new InvalidOrderException("Cannot confirm order in status PENDING")));

        webTestClient.post().uri("/api/orders/{id}/confirm", id.value().toString())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Cannot confirm order in status PENDING");
    }
}
