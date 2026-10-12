package com.tickets.application.usecase.order;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase.GrantComplimentaryCommand;

import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.EventId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrantComplimentaryServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketTransitionRepository transitionRepository;

    @Mock
    private TicketReservationRepository ticketReservationRepository;

    @InjectMocks
    private GrantComplimentaryService service;

    @Test
    void grant_withEnoughAvailableTickets_returnsComplimentaryOrder() {
        EventId eventId = EventId.generate();
        Ticket ticket1 = Ticket.create(eventId, "1");
        Ticket ticket2 = Ticket.create(eventId, "2");

        when(ticketRepository.findAvailableByEvent(eventId, 2))
                .thenReturn(Flux.just(ticket1, ticket2));
        when(ticketReservationRepository.grantComplimentaryAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.Success()));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        GrantComplimentaryCommand command = new GrantComplimentaryCommand(eventId, "customer-123", 2);

        StepVerifier.create(service.grant(command))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
                    assertThat(order.eventId()).isEqualTo(eventId);
                    assertThat(order.customerId()).isEqualTo("customer-123");
                    assertThat(order.quantity()).isEqualTo(2);
                    assertThat(order.ticketIds()).hasSize(2);
                })
                .verifyComplete();

        verify(ticketReservationRepository).grantComplimentaryAtomically(any(), anyInt(), any());
        verify(orderRepository).save(any());
    }

    @Test
    void grant_withNotEnoughAvailableTickets_throwsInvalidEventException() {
        EventId eventId = EventId.generate();
        Ticket ticket1 = Ticket.create(eventId, "1");

        when(ticketRepository.findAvailableByEvent(eventId, 2))
                .thenReturn(Flux.just(ticket1));

        GrantComplimentaryCommand command = new GrantComplimentaryCommand(eventId, "customer-123", 2);

        StepVerifier.create(service.grant(command))
                .verifyError(InvalidEventException.class);
    }

    @Test
    void grant_withNoAvailableTickets_throwsInvalidEventException() {
        EventId eventId = EventId.generate();

        when(ticketRepository.findAvailableByEvent(eventId, 1))
                .thenReturn(Flux.empty());

        GrantComplimentaryCommand command = new GrantComplimentaryCommand(eventId, "customer-123", 1);

        StepVerifier.create(service.grant(command))
                .verifyError(InvalidEventException.class);
    }

    @Test
    void grant_withExactQuantity_returnsComplimentaryOrder() {
        EventId eventId = EventId.generate();
        Ticket ticket1 = Ticket.create(eventId, "1");

        when(ticketRepository.findAvailableByEvent(eventId, 1))
                .thenReturn(Flux.just(ticket1));
        when(ticketReservationRepository.grantComplimentaryAtomically(any(), anyInt(), any()))
                .thenReturn(Mono.just(new ReservationResult.Success()));
        when(transitionRepository.saveAll(any())).thenReturn(Mono.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        GrantComplimentaryCommand command = new GrantComplimentaryCommand(eventId, "vip-customer", 1);

        StepVerifier.create(service.grant(command))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
                    assertThat(order.quantity()).isEqualTo(1);
                    assertThat(order.ticketIds()).hasSize(1);
                })
                .verifyComplete();
    }
}
