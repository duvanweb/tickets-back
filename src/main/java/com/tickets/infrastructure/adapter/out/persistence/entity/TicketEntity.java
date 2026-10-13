package com.tickets.infrastructure.adapter.out.persistence.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/**
 * DynamoDB entity representation of a {@link com.tickets.domain.model.ticket.Ticket}.
 *
 * <p>Uses {@link DynamoDbBean} for the Enhanced Client table schema.
 * Partition key: {@code ticketId}. GSIs:
 * <ul>
 *   <li>{@code event-status-index} (eventId + status) — query tickets by event and status</li>
 *   <li>{@code status-reservedUntil-index} (status + reservedUntil) — scheduler queries for expired reservations</li>
 * </ul></p>
 */
@DynamoDbBean
public class TicketEntity {

    private String ticketId;
    private String eventId;
    private String status;
    private String seatNumber;
    private String orderId;
    private String customerId;
    private String reservedUntil;
    private long version;

    @DynamoDbPartitionKey
    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "event-status-index")
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @DynamoDbSecondarySortKey(indexNames = "event-status-index")
    @DynamoDbSecondaryPartitionKey(indexNames = "status-reservedUntil-index")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @DynamoDbSecondarySortKey(indexNames = "status-reservedUntil-index")
    public String getReservedUntil() {
        return reservedUntil;
    }

    public void setReservedUntil(String reservedUntil) {
        this.reservedUntil = reservedUntil;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
