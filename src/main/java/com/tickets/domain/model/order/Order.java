package com.tickets.domain.model.order;

import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.TicketId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a purchase order for tickets.
 *
 * <p>An order is created in {@link OrderStatus#PENDING} status, published to a
 * queue, and asynchronously processed by a consumer that reserves tickets.
 * Instances are created via {@link #create} (new order) or {@link #reconstitute}
 * (from persistence).</p>
 */
public final class Order {

    private final OrderId id;
    private final EventId eventId;
    private final String customerId;
    private final int quantity;
    private final OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime reservedUntil;
    private final List<TicketId> ticketIds;

    private Order(OrderId id, EventId eventId, String customerId, int quantity,
                  OrderStatus status, LocalDateTime createdAt, LocalDateTime reservedUntil,
                  List<TicketId> ticketIds) {
        this.id = id;
        this.eventId = eventId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.reservedUntil = reservedUntil;
        this.ticketIds = ticketIds;
    }

    /**
     * Factory method that creates a new {@link Order} in {@link OrderStatus#PENDING}
     * with a generated ID, current timestamp, no reservation deadline, and no tickets.
     *
     * @param eventId   the event to buy tickets for
     * @param customerId the customer placing the order
     * @param quantity   the number of tickets requested
     * @return a new {@link Order} in PENDING status
     * @throws InvalidOrderException if validation fails
     */
    public static Order create(EventId eventId, String customerId, int quantity) {
        validate(eventId, customerId, quantity);
        return new Order(OrderId.generate(), eventId, customerId, quantity,
                OrderStatus.PENDING, LocalDateTime.now(), null, List.of());
    }

    /**
     * Rebuilds an {@link Order} from persistence without validation or ID generation.
     *
     * @param id           the existing order ID
     * @param eventId      the event ID
     * @param customerId   the customer ID
     * @param quantity     the ticket quantity
     * @param status       the order status
     * @param createdAt    the creation timestamp
     * @param reservedUntil the reservation deadline (nullable)
     * @param ticketIds    the associated ticket IDs
     * @return a reconstituted {@link Order}
     */
    public static Order reconstitute(OrderId id, EventId eventId, String customerId, int quantity,
                                      OrderStatus status, LocalDateTime createdAt,
                                      LocalDateTime reservedUntil, List<TicketId> ticketIds) {
        return new Order(id, eventId, customerId, quantity, status, createdAt,
                reservedUntil, ticketIds);
    }

    private static void validate(EventId eventId, String customerId, int quantity) {
        if (eventId == null) {
            throw new InvalidOrderException("Event ID must not be null");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException("Customer ID must not be blank");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }
    }

    public OrderId id() {
        return id;
    }

    public EventId eventId() {
        return eventId;
    }

    public String customerId() {
        return customerId;
    }

    public int quantity() {
        return quantity;
    }

    public OrderStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime reservedUntil() {
        return reservedUntil;
    }

    public List<TicketId> ticketIds() {
        return ticketIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", eventId=" + eventId + ", customerId='" + customerId +
                "', quantity=" + quantity + ", status=" + status + ", createdAt=" + createdAt +
                ", reservedUntil=" + reservedUntil + ", ticketIds=" + ticketIds + "}";
    }
}
