package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.ReleaseExpiredReservationsUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.ticket.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReleaseExpiredReservationsUseCase}.
 *
 * <p>Finds tickets with RESERVED status whose reservation deadline has passed,
 * releases them back to AVAILABLE (clearing order/customer associations),
 * updates event counters (release: reserved−N, available+N), and transitions
 * affected orders to EXPIRED.</p>
 */
public final class ReleaseExpiredReservationsService implements ReleaseExpiredReservationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredReservationsService.class);

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;

    public ReleaseExpiredReservationsService(TicketRepository ticketRepository,
                                             EventRepository eventRepository,
                                             OrderRepository orderRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Mono<Integer> releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        return ticketRepository.findExpiredReservations(now)
                .collectList()
                .flatMap(this::releaseTickets);
    }

    private Mono<Integer> releaseTickets(List<Ticket> expiredTickets) {
        if (expiredTickets.isEmpty()) {
            return Mono.just(0);
        }

        log.info("Found {} expired ticket reservations to release", expiredTickets.size());

        List<Ticket> releasedTickets = expiredTickets.stream()
                .map(Ticket::release)
                .toList();

        Map<EventId, List<Ticket>> byEvent = expiredTickets.stream()
                .collect(Collectors.groupingBy(Ticket::eventId));

        Set<String> orderIds = expiredTickets.stream()
                .map(Ticket::orderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return ticketRepository.saveAll(releasedTickets)
                .then(updateEventCounters(byEvent))
                .then(expireOrders(orderIds))
                .then(Mono.just(expiredTickets.size()));
    }

    private Mono<Void> updateEventCounters(Map<EventId, List<Ticket>> byEvent) {
        return Flux.fromIterable(byEvent.entrySet())
                .flatMap(entry -> {
                    EventId eventId = entry.getKey();
                    int quantity = entry.getValue().size();
                    return eventRepository.findById(eventId)
                            .flatMap(event -> {
                                var updated = event.release(quantity);
                                return eventRepository.save(updated);
                            });
                })
                .then();
    }

    private Mono<Void> expireOrders(Set<String> orderIds) {
        return Flux.fromIterable(orderIds)
                .flatMap(orderIdStr -> {
                    OrderId orderId = OrderId.of(orderIdStr);
                    return orderRepository.findById(orderId)
                            .flatMap(order -> {
                                if (order.canTransitionTo(
                                        com.tickets.domain.model.order.OrderStatus.EXPIRED)) {
                                    Order expired = order.expire();
                                    return orderRepository.save(expired);
                                }
                                return Mono.just(order);
                            });
                })
                .then();
    }
}
