package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
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
class ReleaseExpiredReservationsServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReleaseExpiredReservationsService service;

    @Test
    void releaseExpired_withExpiredTickets_releasesTicketsAndExpiresOrders() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        LocalDateTime now = LocalDateTime.now();

        Ticket expiredTicket1 = Ticket.reconstitute(TicketId.generate(), eventId,
                com.tickets.domain.model.ticket.TicketStatus.RESERVED, "1",
                orderId.value().toString(), "customer-123", now.minusMinutes(5), 1);
        Ticket expiredTicket2 = Ticket.reconstitute(TicketId.generate(), eventId,
                com.tickets.domain.model.ticket.TicketStatus.RESERVED, "2",
                orderId.value().toString(), "customer-123", now.minusMinutes(5), 1);

        Event event = Event.create("Concert", now.plusDays(30), "Stadium", 1000);
        Event eventWithReserved = Event.reconstitute(event.id(), "Concert", now.plusDays(30),
                "Stadium", 1000, 998, 2, 0, 0);

        Order reservedOrder = Order.reconstitute(orderId, eventId, "customer-123", 2,
                OrderStatus.RESERVED, now.minusMinutes(10), now.minusMinutes(5),
                List.of(expiredTicket1.id(), expiredTicket2.id()));

        when(ticketRepository.findExpiredReservations(any())).thenReturn(
                Flux.fromIterable(List.of(expiredTicket1, expiredTicket2)));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.empty());
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(eventWithReserved));
        when(eventRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(reservedOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.releaseExpired())
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();

        verify(ticketRepository).saveAll(any());
        verify(eventRepository).save(any());
        verify(orderRepository).save(any());
    }

    @Test
    void releaseExpired_withNoExpiredTickets_returnsZero() {
        when(ticketRepository.findExpiredReservations(any())).thenReturn(Flux.empty());

        StepVerifier.create(service.releaseExpired())
                .assertNext(count -> assertThat(count).isZero())
                .verifyComplete();

        verify(ticketRepository, never()).saveAll(any());
        verify(eventRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }
}
