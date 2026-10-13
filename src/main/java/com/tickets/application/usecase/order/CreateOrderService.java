package com.tickets.application.usecase.order;
import com.tickets.application.port.in.order.CreateOrderUseCase.CreateOrderCommand;

import com.tickets.application.port.in.order.CreateOrderUseCase;
import com.tickets.application.port.out.publishers.MessagePublisher;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link CreateOrderUseCase}.
 *
 * <p>Validates that the event exists, creates a new {@link Order} in
 * {@code PENDING} status, persists it through the {@link OrderRepository}
 * port, serializes the order to a JSON payload, and publishes it via the
 * {@link MessagePublisher} port for asynchronous processing.</p>
 */
public final class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository repository;
    private final EventRepository eventRepository;
    private final MessagePublisher messagePublisher;

    public CreateOrderService(OrderRepository repository,
                              EventRepository eventRepository,
                              MessagePublisher messagePublisher) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.messagePublisher = messagePublisher;
    }

    @Override
    public Mono<Order> create(CreateOrderCommand command) {
        return eventRepository.findById(command.eventId())
                .switchIfEmpty(Mono.error(new EventNotFoundException(
                        "Event not found with id: " + command.eventId())))
                .then(Mono.fromCallable(() ->
                        Order.create(command.eventId(), command.customerId(), command.quantity())))
                .flatMap(order -> repository.save(order)
                        .flatMap(savedOrder -> messagePublisher.publish(serializePayload(savedOrder))
                                .then(Mono.just(savedOrder))));
    }

    private String serializePayload(Order order) {
        String payload = "{\"orderId\":\"" + order.id().value() + "\""
                + ",\"eventId\":\"" + order.eventId().value() + "\""
                + ",\"customerId\":\"" + order.customerId() + "\""
                + ",\"quantity\":" + order.quantity()
                + ",\"status\":\"" + order.status() + "\""
                + ",\"createdAt\":\"" + order.createdAt() + "\"}";
        return "{\"type\":\"order-created\",\"payload\":" + payload + "}";
    }
}
