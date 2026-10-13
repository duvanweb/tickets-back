package com.tickets.infrastructure.adapter.in.web;

import com.tickets.application.port.in.order.ConfirmOrderUseCase;
import com.tickets.application.port.in.order.CreateOrderUseCase;
import com.tickets.application.port.in.order.GetOrderUseCase;
import com.tickets.domain.model.order.OrderId;
import com.tickets.infrastructure.adapter.in.web.dto.order.CreateOrderRequest;
import com.tickets.infrastructure.adapter.in.web.dto.order.OrderDtoMapper;
import com.tickets.infrastructure.adapter.in.web.dto.order.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for order endpoints.
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@code POST /api/orders} — creates an order (202 Accepted)</li>
 *   <li>{@code GET /api/orders/{id}} — retrieves an order by ID</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           GetOrderUseCase getOrderUseCase,
                           ConfirmOrderUseCase confirmOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.confirmOrderUseCase = confirmOrderUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> create(@Valid @RequestBody Mono<CreateOrderRequest> request) {
        return request
                .map(OrderDtoMapper::toCommand)
                .flatMap(createOrderUseCase::create)
                .map(order -> ResponseEntity
                        .accepted()
                        .body(OrderDtoMapper.toResponse(order)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderResponse>> getById(@PathVariable String id) {
        return getOrderUseCase.getById(OrderId.of(id))
                .map(order -> ResponseEntity.ok(OrderDtoMapper.toResponse(order)));
    }

    @PostMapping("/{id}/confirm")
    public Mono<ResponseEntity<OrderResponse>> confirm(@PathVariable String id) {
        return confirmOrderUseCase.confirm(OrderId.of(id))
                .map(order -> ResponseEntity.ok(OrderDtoMapper.toResponse(order)));
    }
}
