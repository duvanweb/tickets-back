package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOrdersByEventServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ListOrdersByEventService service;

    @Test
    void findByEventId_whenEventExists_returnsOrders() {
        EventId eventId = EventId.generate();
        Event event = Event.reconstitute(eventId, "Concierto", LocalDateTime.of(2026, 12, 15, 19, 30), "Estadio", 1000, 1000, 0, 0, 0);

        Order order1 = Order.reconstitute(OrderId.generate(), eventId, "customer-1", 2,
                com.tickets.domain.model.order.OrderStatus.SOLD, LocalDateTime.now(), null, java.util.List.of());
        Order order2 = Order.reconstitute(OrderId.generate(), eventId, "customer-2", 3,
                com.tickets.domain.model.order.OrderStatus.PENDING, LocalDateTime.now(), null, java.util.List.of());

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.just(order1, order2));

        StepVerifier.create(service.findByEventId(eventId))
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }

    @Test
    void findByEventId_whenEventNotFound_throwsEventNotFoundException() {
        EventId eventId = EventId.generate();

        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());

        StepVerifier.create(service.findByEventId(eventId))
                .verifyError(EventNotFoundException.class);
    }

    @Test
    void findByEventId_whenEventExistsButNoOrders_returnsEmptyFlux() {
        EventId eventId = EventId.generate();
        Event event = Event.reconstitute(eventId, "Concierto", LocalDateTime.of(2026, 12, 15, 19, 30), "Estadio", 1000, 1000, 0, 0, 0);

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(orderRepository.findByEventId(eventId)).thenReturn(Flux.empty());

        StepVerifier.create(service.findByEventId(eventId))
                .verifyComplete();
    }
}
