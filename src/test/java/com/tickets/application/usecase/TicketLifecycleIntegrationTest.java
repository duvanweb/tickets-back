package com.tickets.application.usecase;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.application.usecase.event.model.EventAvailability;
import com.tickets.application.usecase.event.GetAvailabilityService;
import com.tickets.application.usecase.order.ConfirmOrderService;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase.GrantComplimentaryCommand;
import com.tickets.application.usecase.order.GrantComplimentaryService;
import com.tickets.application.port.in.order.ProcessOrderUseCase.ProcessOrderCommand;
import com.tickets.application.usecase.order.ProcessOrderService;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test exercising the full ticket lifecycle:
 * availability → order → process (reserve) → confirm → SOLD.
 *
 * <p>Uses thread-safe in-memory repositories to wire all use cases together
 * with their real implementations, verifying the integration between
 * domain model, use cases, and repository contracts.</p>
 */
class TicketLifecycleIntegrationTest {

    @Test
    void fullLifecycle_availabilityToSold_endToEnd() {
        int capacity = 50;
        EventId eventId = EventId.generate();
        Event event = Event.create("Concert", LocalDateTime.now().plusDays(30), "Stadium", capacity);

        List<Ticket> tickets = java.util.stream.IntStream.rangeClosed(1, capacity)
                .mapToObj(i -> Ticket.create(eventId, String.valueOf(i)))
                .toList();

        InMemoryEventRepository eventRepo = new InMemoryEventRepository(event);
        InMemoryTicketRepository ticketRepo = new InMemoryTicketRepository(tickets);
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemoryTransitionRepository transitionRepo = new InMemoryTransitionRepository();
        InMemoryTicketReservationRepository reservationRepo = new InMemoryTicketReservationRepository(
                ticketRepo, eventRepo);

        GetAvailabilityService availabilityService = new GetAvailabilityService(eventRepo);
        ProcessOrderService processService = new ProcessOrderService(
                orderRepo, ticketRepo, eventRepo, transitionRepo, reservationRepo);
        ConfirmOrderService confirmService = new ConfirmOrderService(orderRepo, ticketRepo, eventRepo, transitionRepo);

        // 1. Check availability — all tickets available
        EventAvailability availability = availabilityService.getAvailability(eventId).block();
        assertThat(availability.available()).isEqualTo(capacity);
        assertThat(availability.reserved()).isZero();
        assertThat(availability.sold()).isZero();

        // 2. Create and process an order for 3 tickets
        Order order = Order.create(eventId, "customer-123", 3);
        OrderId orderId = order.id();
        orderRepo.store(order);

        Order processedOrder = processService.process(
                new ProcessOrderCommand(orderId, eventId, "customer-123", 3)).block();

        assertThat(processedOrder.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(processedOrder.ticketIds()).hasSize(3);

        // 3. Check availability — 3 tickets reserved
        EventAvailability afterReserve = availabilityService.getAvailability(eventId).block();
        assertThat(afterReserve.available()).isEqualTo(capacity - 3);
        assertThat(afterReserve.reserved()).isEqualTo(3);

        // 4. Confirm the order → SOLD
        Order soldOrder = confirmService.confirm(orderId).block();

        assertThat(soldOrder.status()).isEqualTo(OrderStatus.SOLD);

        // 5. Check availability — 3 tickets sold
        EventAvailability afterSale = availabilityService.getAvailability(eventId).block();
        assertThat(afterSale.available()).isEqualTo(capacity - 3);
        assertThat(afterSale.reserved()).isZero();
        assertThat(afterSale.sold()).isEqualTo(3);

        // 6. Verify transitions were logged
        List<TicketTransition> transitions = processedOrder.ticketIds().stream()
                .flatMap(tid -> transitionRepo.findByTicketId(tid).toStream())
                .toList();
        assertThat(transitions).hasSize(6);

        long reserveTransitions = transitions.stream()
                .filter(t -> t.fromStatus() == TicketStatus.AVAILABLE && t.toStatus() == TicketStatus.RESERVED)
                .count();
        long soldTransitions = transitions.stream()
                .filter(t -> t.fromStatus() == TicketStatus.RESERVED && t.toStatus() == TicketStatus.SOLD)
                .count();
        assertThat(reserveTransitions).isEqualTo(3);
        assertThat(soldTransitions).isEqualTo(3);
    }

    @Test
    void complimentaryFlow_availableToComplimentary_endToEnd() {
        int capacity = 20;
        EventId eventId = EventId.generate();
        Event event = Event.create("VIP Event", LocalDateTime.now().plusDays(30), "Arena", capacity);

        List<Ticket> tickets = java.util.stream.IntStream.rangeClosed(1, capacity)
                .mapToObj(i -> Ticket.create(eventId, String.valueOf(i)))
                .toList();

        InMemoryEventRepository eventRepo = new InMemoryEventRepository(event);
        InMemoryTicketRepository ticketRepo = new InMemoryTicketRepository(tickets);
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        InMemoryTransitionRepository transitionRepo = new InMemoryTransitionRepository();

        GetAvailabilityService availabilityService = new GetAvailabilityService(eventRepo);
        GrantComplimentaryService grantService = new GrantComplimentaryService(
                ticketRepo, orderRepo, transitionRepo,
                new InMemoryTicketReservationRepository(ticketRepo, eventRepo));

        // Grant 2 complimentary tickets
        Order complimentaryOrder = grantService.grant(
                new GrantComplimentaryCommand(eventId, "vip-customer", 2)).block();

        assertThat(complimentaryOrder.status()).isEqualTo(OrderStatus.COMPLIMENTARY);
        assertThat(complimentaryOrder.quantity()).isEqualTo(2);

        // Verify availability reflects complimentary
        EventAvailability availability = availabilityService.getAvailability(eventId).block();
        assertThat(availability.available()).isEqualTo(capacity - 2);
        assertThat(availability.complimentary()).isEqualTo(2);
    }

    // ─── In-memory repositories ─────────────────────────────────────────

    private static final class InMemoryEventRepository implements EventRepository {
        private Event event;
        private final Object lock = new Object();

        InMemoryEventRepository(Event event) { this.event = event; }

        @Override
        public Mono<Event> save(Event event) {
            synchronized (lock) { this.event = event; }
            return Mono.just(event);
        }

        @Override
        public Mono<Event> findById(EventId id) {
            synchronized (lock) { return Mono.just(this.event); }
        }

        @Override
        public Flux<Event> findAll() { return Flux.just(event); }

        @Override
        public Mono<Boolean> existsByName(String name) { return Mono.just(false); }

        Event getEvent() {
            synchronized (lock) { return this.event; }
        }

        boolean tryReserve(int quantity) {
            synchronized (lock) {
                if (this.event.available() >= quantity) {
                    this.event = this.event.reserve(quantity);
                    return true;
                }
                return false;
            }
        }

        boolean tryGrantComplimentary(int quantity) {
            synchronized (lock) {
                if (this.event.available() >= quantity) {
                    this.event = this.event.grantComplimentary(quantity);
                    return true;
                }
                return false;
            }
        }

        void restore(int quantity) {
            synchronized (lock) {
                this.event = this.event.release(quantity);
            }
        }
    }

    private static final class InMemoryTicketRepository implements TicketRepository {
        private final Map<TicketId, Ticket> tickets = new ConcurrentHashMap<>();

        InMemoryTicketRepository(List<Ticket> initial) {
            initial.forEach(t -> tickets.put(t.id(), t));
        }

        @Override
        public Flux<Ticket> saveAll(List<Ticket> tickets) {
            tickets.forEach(t -> this.tickets.put(t.id(), t));
            return Flux.fromIterable(tickets);
        }

        @Override
        public Mono<Ticket> findById(TicketId id) {
            return Mono.justOrEmpty(tickets.get(id));
        }

        @Override
        public Flux<Ticket> findAvailableByEvent(EventId eventId, int limit) {
            List<Ticket> available = tickets.values().stream()
                    .filter(t -> t.eventId().equals(eventId))
                    .filter(t -> t.status() == TicketStatus.AVAILABLE)
                    .limit(limit)
                    .collect(Collectors.toList());
            return Flux.fromIterable(available);
        }

        @Override
        public Flux<Ticket> findExpiredReservations(LocalDateTime cutoff) {
            return Flux.empty();
        }

        Ticket getTicket(TicketId id) {
            return tickets.get(id);
        }

        void putTicket(Ticket ticket) {
            tickets.put(ticket.id(), ticket);
        }
    }

    private static final class InMemoryTicketReservationRepository implements TicketReservationRepository {
        private final InMemoryTicketRepository ticketRepo;
        private final InMemoryEventRepository eventRepo;
        private final Object txLock = new Object();

        InMemoryTicketReservationRepository(InMemoryTicketRepository ticketRepo,
                                             InMemoryEventRepository eventRepo) {
            this.ticketRepo = ticketRepo;
            this.eventRepo = eventRepo;
        }

        @Override
        public Mono<ReservationResult> reserveTicketsAtomically(EventId eventId,
                                                                 int quantity,
                                                                 List<Ticket> reservedTickets) {
            return Mono.fromCallable(() -> {
                synchronized (txLock) {
                    if (!eventRepo.tryReserve(quantity)) {
                        return new ReservationResult.EventConditionFailed();
                    }
                    for (Ticket reserved : reservedTickets) {
                        Ticket current = ticketRepo.getTicket(reserved.id());
                        if (current == null || current.status() != TicketStatus.AVAILABLE) {
                            eventRepo.save(eventRepo.getEvent().release(quantity));
                            return new ReservationResult.TicketConditionFailed();
                        }
                    }
                    for (Ticket reserved : reservedTickets) {
                        ticketRepo.putTicket(reserved);
                    }
                    return new ReservationResult.Success();
                }
            });
        }

        @Override
        public Mono<ReservationResult> grantComplimentaryAtomically(EventId eventId,
                                                                    int quantity,
                                                                    List<Ticket> complimentaryTickets) {
            return Mono.fromCallable(() -> {
                synchronized (txLock) {
                    if (!eventRepo.tryGrantComplimentary(quantity)) {
                        return new ReservationResult.EventConditionFailed();
                    }
                    for (Ticket complimentary : complimentaryTickets) {
                        Ticket current = ticketRepo.getTicket(complimentary.id());
                        if (current == null || current.status() != TicketStatus.AVAILABLE) {
                            eventRepo.restore(quantity);
                            return new ReservationResult.TicketConditionFailed();
                        }
                    }
                    for (Ticket complimentary : complimentaryTickets) {
                        ticketRepo.putTicket(complimentary);
                    }
                    return new ReservationResult.Success();
                }
            });
        }
    }

    private static final class InMemoryOrderRepository implements OrderRepository {
        private final Map<OrderId, Order> orders = new ConcurrentHashMap<>();

        @Override
        public Mono<Order> save(Order order) {
            orders.put(order.id(), order);
            return Mono.just(order);
        }

        @Override
        public Mono<Order> findById(OrderId id) {
            return Mono.justOrEmpty(orders.get(id));
        }

        @Override
        public Flux<Order> findByEventId(EventId eventId) {
            return Flux.fromStream(orders.values().stream()
                    .filter(order -> order.eventId().equals(eventId)));
        }

        void store(Order order) { orders.put(order.id(), order); }
    }

    private static final class InMemoryTransitionRepository implements TicketTransitionRepository {
        private final Map<TicketId, List<TicketTransition>> byTicket = new ConcurrentHashMap<>();

        @Override
        public Mono<Void> saveAll(List<TicketTransition> transitions) {
            transitions.forEach(t -> byTicket.merge(t.ticketId(),
                    List.of(t),
                    (old, neu) -> {
                        var combined = new java.util.ArrayList<>(old);
                        combined.addAll(neu);
                        return combined;
                    }));
            return Mono.empty();
        }

        @Override
        public Flux<TicketTransition> findByTicketId(TicketId ticketId) {
            return Flux.fromIterable(byTicket.getOrDefault(ticketId, List.of()));
        }
    }
}
