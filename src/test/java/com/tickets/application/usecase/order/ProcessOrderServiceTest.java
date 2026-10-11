package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.Ticket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTransitionRepository transitionRepository;

    @InjectMocks
    private ProcessOrderService service;

    @Test
    void process_whenEnoughAvailableTickets_reservesOrderAndTickets() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> availableTickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"),
                Ticket.create(eventId, "3"));

        Event event = Event.create("Concert", LocalDateTime.of(2026, 12, 15, 19, 30), "Stadium", 1000);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 2)).thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.fromIterable(availableTickets.subList(0, 2)));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any())).thenReturn(Mono.just(event));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(result.ticketIds()).hasSize(2);
                    assertThat(result.reservedUntil()).isNotNull();
                })
                .verifyComplete();

        verify(ticketRepository).saveAll(any());
        verify(eventRepository).save(any());
        verify(orderRepository).save(any());
    }

    @Test
    void process_whenNotEnoughTickets_rejectsOrder() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 5,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> availableTickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 5)).thenReturn(Flux.fromIterable(availableTickets));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 5);

        StepVerifier.create(service.process(command))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
                })
                .verifyComplete();

        verify(ticketRepository, never()).saveAll(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void process_whenOrderNotFound_throwsOrderNotFoundException() {
        OrderId orderId = OrderId.generate();
        EventId eventId = EventId.generate();

        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .verifyError(OrderNotFoundException.class);
    }

    @Test
    void process_whenExactQuantityAvailable_reservesOrder() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> availableTickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));

        Event event = Event.create("Concert", LocalDateTime.of(2026, 12, 15, 19, 30), "Stadium", 1000);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 2)).thenReturn(Flux.fromIterable(availableTickets));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.fromIterable(availableTickets));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.save(any())).thenReturn(Mono.just(event));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(result.ticketIds()).hasSize(2);
                })
                .verifyComplete();
    }
}
