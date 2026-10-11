package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ConfirmOrderService service;

    @Test
    void confirm_withValidReservedOrder_transitionsToSold() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        TicketId ticketId1 = TicketId.generate();
        TicketId ticketId2 = TicketId.generate();
        LocalDateTime now = LocalDateTime.now();

        Order reservedOrder = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.RESERVED, now.minusMinutes(5), now.plusMinutes(5),
                List.of(ticketId1, ticketId2));

        Ticket ticket1 = Ticket.reconstitute(ticketId1, eventId,
                com.tickets.domain.model.ticket.TicketStatus.RESERVED, "1",
                orderId.value().toString(), "customer-123", now.plusMinutes(5), 1);
        Ticket ticket2 = Ticket.reconstitute(ticketId2, eventId,
                com.tickets.domain.model.ticket.TicketStatus.RESERVED, "2",
                orderId.value().toString(), "customer-123", now.plusMinutes(5), 1);

        Event event = Event.create("Concert", now.plusDays(30), "Stadium", 1000);
        Event eventWithReserved = Event.reconstitute(event.id(), "Concert", now.plusDays(30),
                "Stadium", 1000, 998, 2, 0, 0);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(reservedOrder));
        when(ticketRepository.findById(ticketId1)).thenReturn(Mono.just(ticket1));
        when(ticketRepository.findById(ticketId2)).thenReturn(Mono.just(ticket2));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.empty());
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(eventWithReserved));
        when(eventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.confirm(orderId))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.SOLD);
                    assertThat(order.id()).isEqualTo(orderId);
                })
                .verifyComplete();

        verify(ticketRepository).saveAll(any());
        verify(eventRepository).save(any());
        verify(orderRepository).save(any());
    }

    @Test
    void confirm_whenOrderNotFound_throwsOrderNotFoundException() {
        OrderId orderId = OrderId.generate();

        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(service.confirm(orderId))
                .verifyError(OrderNotFoundException.class);
    }

    @Test
    void confirm_withPendingOrder_throwsInvalidOrderException() {
        OrderId orderId = OrderId.generate();
        Order pendingOrder = Order.create(EventId.generate(), "customer-123", 2);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(pendingOrder));

        StepVerifier.create(service.confirm(orderId))
                .verifyError(InvalidOrderException.class);
    }

    @Test
    void confirm_withExpiredReservation_throwsInvalidOrderException() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        LocalDateTime now = LocalDateTime.now();

        Order expiredOrder = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.RESERVED, now.minusHours(1), now.minusMinutes(5),
                List.of(TicketId.generate(), TicketId.generate()));

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(expiredOrder));

        StepVerifier.create(service.confirm(orderId))
                .verifyError(InvalidOrderException.class);
    }
}
