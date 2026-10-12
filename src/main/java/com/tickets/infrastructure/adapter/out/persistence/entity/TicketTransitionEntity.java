package com.tickets.infrastructure.adapter.out.persistence.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/**
 * DynamoDB entity for the {@code ticket_transitions} audit table.
 *
 * <p>PK: {@code transitionId}. GSI {@code ticket-id-index}: {@code ticketId} (HASH),
 * {@code timestamp} (RANGE) for querying the transition history of a ticket.</p>
 */
@DynamoDbBean
public class TicketTransitionEntity {

    private String transitionId;
    private String ticketId;
    private String fromStatus;
    private String toStatus;
    private String orderId;
    private String timestamp;

    @DynamoDbPartitionKey
    public String getTransitionId() { return transitionId; }
    public void setTransitionId(String transitionId) { this.transitionId = transitionId; }

    @DynamoDbSecondaryPartitionKey(indexNames = "ticket-id-index")
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @DynamoDbSecondarySortKey(indexNames = "ticket-id-index")
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
