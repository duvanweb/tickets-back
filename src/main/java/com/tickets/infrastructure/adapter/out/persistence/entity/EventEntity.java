package com.tickets.infrastructure.adapter.out.persistence.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * DynamoDB entity representation of an {@link com.tickets.domain.model.event.Event}.
 *
 * <p>Uses {@link DynamoDbBean} for the Enhanced Client table schema.</p>
 */
@DynamoDbBean
public class EventEntity {

    private String eventId;
    private String name;
    private String eventDate;
    private String place;
    private int totalCapacity;
    private int available;
    private int reserved;
    private int sold;
    private int complimentary;

    @DynamoDbPartitionKey
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "name-index")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getSold() {
        return sold;
    }

    public void setSold(int sold) {
        this.sold = sold;
    }

    public int getComplimentary() {
        return complimentary;
    }

    public void setComplimentary(int complimentary) {
        this.complimentary = complimentary;
    }
}
