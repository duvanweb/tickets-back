package com.tickets.application.usecase.order;
import com.tickets.application.port.in.order.ProcessOrderUseCase.ProcessOrderCommand;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.OrderNotFoundException;
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

    @Mock
    private TicketReservationRepository ticketReservationRepository;

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

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 2)).thenReturn(Flux.fromIterable(availableTickets));
        when(ticketReservationRepository.reserveTicketsAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.Success()));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(result.ticketIds()).hasSize(2);
                    assertThat(result.reservedUntil()).isNotNull();
                })
                .verifyComplete();

        verify(ticketReservationRepository).reserveTicketsAtomically(any(), anyInt(), any());
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
                .assertNext(result -> assertThat(result.status()).isEqualTo(OrderStatus.REJECTED))
                .verifyComplete();

        verify(ticketReservationRepository, never()).reserveTicketsAtomically(any(), anyInt(), any());
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

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 2)).thenReturn(Flux.fromIterable(availableTickets));
        when(ticketReservationRepository.reserveTicketsAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.Success()));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(OrderStatus.RESERVED);
                    assertThat(result.ticketIds()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    void process_whenEventConditionFailed_rejectsOrder() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> availableTickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 2)).thenReturn(Flux.fromIterable(availableTickets));
        when(ticketReservationRepository.reserveTicketsAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.EventConditionFailed()));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 2);

        StepVerifier.create(service.process(command))
                .assertNext(result -> assertThat(result.status()).isEqualTo(OrderStatus.REJECTED))
                .verifyComplete();

        // Should not retry on EventConditionFailed
        verify(ticketRepository, times(1)).findAvailableByEvent(any(), anyInt());
    }

    @Test
    void process_whenTicketConditionFailedThenSuccess_retriesAndSucceeds() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 1,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> firstBatch = List.of(Ticket.create(eventId, "1"));
        List<Ticket> secondBatch = List.of(Ticket.create(eventId, "2"));

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 1))
                .thenReturn(Flux.fromIterable(firstBatch))
                .thenReturn(Flux.fromIterable(secondBatch));
        when(ticketReservationRepository.reserveTicketsAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.TicketConditionFailed()))
                .thenReturn(Mono.just(new ReservationResult.Success()));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 1);

        StepVerifier.create(service.process(command))
                .assertNext(result -> assertThat(result.status()).isEqualTo(OrderStatus.RESERVED))
                .verifyComplete();

        verify(ticketRepository, times(2)).findAvailableByEvent(any(), anyInt());
    }

    @Test
    void process_whenTicketConditionFailedExceedsMaxRetries_rejectsOrder() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        Order order = Order.reconstitute(orderId, eventId, "customer-123", 1,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());

        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"));

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));
        when(ticketRepository.findAvailableByEvent(eventId, 1)).thenReturn(Flux.fromIterable(tickets));
        when(ticketReservationRepository.reserveTicketsAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.TicketConditionFailed()));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 1);

        StepVerifier.create(service.process(command))
                .assertNext(result -> assertThat(result.status()).isEqualTo(OrderStatus.REJECTED))
                .verifyComplete();

        // 1 initial + 2 retries = 3 attempts (MAX_RETRIES=3, attempt 4 > 3 → reject)
        verify(ticketRepository, times(3)).findAvailableByEvent(any(), anyInt());
    }

    @Test
    void process_whenQuantityExceeds99_throwsInvalidOrderException() {
        OrderId orderId = OrderId.generate();
        EventId eventId = EventId.generate();

        ProcessOrderCommand command = new ProcessOrderCommand(orderId, eventId, "customer-123", 100);

        StepVerifier.create(service.process(command))
                .verifyError(InvalidOrderException.class);

        verifyNoInteractions(orderRepository, ticketRepository, ticketReservationRepository);
    }
}
