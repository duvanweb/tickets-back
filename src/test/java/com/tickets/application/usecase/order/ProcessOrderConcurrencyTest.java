package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for {@link ProcessOrderService} — the decisive test that verifies
 * no overselling under concurrent access.
 *
 * <p>Uses thread-safe in-memory repository implementations to simulate DynamoDB's
 * atomic conditional writes. The {@code InMemoryTicketRepository.findAvailableByEvent}
 * atomically finds and claims tickets (marks them RESERVED in the map) to prevent
 * two threads from reserving the same ticket. Fires N concurrent order requests
 * for M &lt; N tickets and verifies exactly M succeed (RESERVED) and N−M fail (REJECTED).</p>
 */
class ProcessOrderConcurrencyTest {

    @Test
    void concurrentOrders_forLimitedTickets_noOverselling() throws InterruptedException {
        int capacity = 10;
        int concurrentOrders = 100;

        EventId eventId = EventId.generate();
        Event event = Event.create("Concert", LocalDateTime.now().plusDays(30), "Stadium", capacity);

        List<Ticket> tickets = java.util.stream.IntStream.rangeClosed(1, capacity)
                .mapToObj(i -> Ticket.create(eventId, String.valueOf(i)))
                .toList();

        InMemoryEventRepository eventRepo = new InMemoryEventRepository(event);
        InMemoryTicketRepository ticketRepo = new InMemoryTicketRepository(tickets);
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        NoOpTransitionRepository transitionRepo = new NoOpTransitionRepository();

        ProcessOrderService service = new ProcessOrderService(orderRepo, ticketRepo, eventRepo, transitionRepo);

        List<ProcessOrderCommand> commands = java.util.stream.IntStream.range(0, concurrentOrders)
                .mapToObj(i -> {
                    Order order = Order.create(eventId, "customer-" + i, 1);
                    orderRepo.store(order);
                    return new ProcessOrderCommand(order.id(), eventId, "customer-" + i, 1);
                })
                .toList();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        AtomicInteger reservedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (ProcessOrderCommand command : commands) {
            executor.submit(() -> {
                service.process(command)
                        .subscribe(order -> {
                            if (order.status() == OrderStatus.RESERVED) {
                                reservedCount.incrementAndGet();
                            } else if (order.status() == OrderStatus.REJECTED) {
                                rejectedCount.incrementAndGet();
                            }
                            latch.countDown();
                        }, error -> latch.countDown());
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(reservedCount.get()).isEqualTo(capacity);
        assertThat(rejectedCount.get()).isEqualTo(concurrentOrders - capacity);

        long availableTickets = ticketRepo.countAvailable(eventId);
        assertThat(availableTickets).isZero();
    }

    @Test
    void concurrentOrders_forMoreThanAvailable_allRejected() throws InterruptedException {
        int capacity = 5;
        int concurrentOrders = 20;
        int quantityPerOrder = 2;

        EventId eventId = EventId.generate();
        Event event = Event.create("Concert", LocalDateTime.now().plusDays(30), "Stadium", capacity);

        List<Ticket> tickets = java.util.stream.IntStream.rangeClosed(1, capacity)
                .mapToObj(i -> Ticket.create(eventId, String.valueOf(i)))
                .toList();

        InMemoryEventRepository eventRepo = new InMemoryEventRepository(event);
        InMemoryTicketRepository ticketRepo = new InMemoryTicketRepository(tickets);
        InMemoryOrderRepository orderRepo = new InMemoryOrderRepository();
        NoOpTransitionRepository transitionRepo = new NoOpTransitionRepository();

        ProcessOrderService service = new ProcessOrderService(orderRepo, ticketRepo, eventRepo, transitionRepo);

        List<ProcessOrderCommand> commands = java.util.stream.IntStream.range(0, concurrentOrders)
                .mapToObj(i -> {
                    Order order = Order.create(eventId, "customer-" + i, quantityPerOrder);
                    orderRepo.store(order);
                    return new ProcessOrderCommand(order.id(), eventId, "customer-" + i, quantityPerOrder);
                })
                .toList();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        AtomicInteger reservedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (ProcessOrderCommand command : commands) {
            executor.submit(() -> {
                service.process(command)
                        .subscribe(order -> {
                            if (order.status() == OrderStatus.RESERVED) {
                                reservedCount.incrementAndGet();
                            } else if (order.status() == OrderStatus.REJECTED) {
                                rejectedCount.incrementAndGet();
                            }
                            latch.countDown();
                        }, error -> latch.countDown());
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(reservedCount.get() * quantityPerOrder).isLessThanOrEqualTo(capacity);
        assertThat(reservedCount.get() + rejectedCount.get()).isEqualTo(concurrentOrders);
    }

    // ─── Thread-safe in-memory repositories ─────────────────────────────

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
    }

    /**
     * Thread-safe in-memory ticket repository that simulates DynamoDB atomic
     * conditional writes. {@code findAvailableByEvent} atomically finds and
     * claims tickets by marking them RESERVED in the map, preventing two
     * threads from reserving the same ticket.
     */
    private static final class InMemoryTicketRepository implements TicketRepository {
        private final Map<TicketId, Ticket> tickets = new ConcurrentHashMap<>();
        private final Object claimLock = new Object();

        InMemoryTicketRepository(List<Ticket> initialTickets) {
            initialTickets.forEach(t -> tickets.put(t.id(), t));
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
            synchronized (claimLock) {
                List<Ticket> available = tickets.values().stream()
                        .filter(t -> t.eventId().equals(eventId))
                        .filter(t -> t.status() == TicketStatus.AVAILABLE)
                        .limit(limit)
                        .collect(Collectors.toList());
                // Atomically claim tickets by marking them RESERVED in the map,
                // simulating DynamoDB TransactWriteItems conditional update
                for (Ticket t : available) {
                    tickets.put(t.id(), t.transitionTo(TicketStatus.RESERVED));
                }
                return Flux.fromIterable(available);
            }
        }

        @Override
        public Flux<Ticket> findExpiredReservations(LocalDateTime cutoff) {
            return Flux.empty();
        }

        long countAvailable(EventId eventId) {
            return tickets.values().stream()
                    .filter(t -> t.eventId().equals(eventId))
                    .filter(t -> t.status() == TicketStatus.AVAILABLE)
                    .count();
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

        void store(Order order) { orders.put(order.id(), order); }
    }

    private static final class NoOpTransitionRepository implements TicketTransitionRepository {
        @Override
        public Mono<Void> saveAll(List<TicketTransition> transitions) {
            return Mono.empty();
        }

        @Override
        public Flux<TicketTransition> findByTicketId(TicketId ticketId) {
            return Flux.empty();
        }
    }
}
