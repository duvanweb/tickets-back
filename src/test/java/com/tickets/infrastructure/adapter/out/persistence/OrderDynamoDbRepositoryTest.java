package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDynamoDbRepositoryTest {

    @Mock
    private DynamoDbTable<OrderEntity> orderTable;

    @InjectMocks
    private OrderDynamoDbRepository repository;

    private OrderEntity buildEntity(String orderId) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setEventId(EventId.generate().value().toString());
        entity.setCustomerId("customer-123");
        entity.setQuantity(3);
        entity.setStatus("PENDING");
        entity.setCreatedAt(java.time.LocalDateTime.now().toString());
        return entity;
    }

    @Test
    void save_putsEntityAndReturnsOrder() {
        Order order = Order.create(EventId.generate(), "customer-123", 3);

        StepVerifier.create(repository.save(order))
                .assertNext(saved -> {
                    assertThat(saved.customerId()).isEqualTo("customer-123");
                    assertThat(saved.quantity()).isEqualTo(3);
                })
                .verifyComplete();

        verify(orderTable).putItem(any(OrderEntity.class));
    }

    @Test
    void findById_whenEntityExists_returnsMappedOrder() {
        OrderId id = OrderId.generate();
        OrderEntity entity = buildEntity(id.value().toString());
        when(orderTable.getItem(any(Key.class))).thenReturn(entity);

        StepVerifier.create(repository.findById(id))
                .assertNext(order -> {
                    assertThat(order.id()).isEqualTo(id);
                    assertThat(order.status().name()).isEqualTo("PENDING");
                })
                .verifyComplete();
    }

    @Test
    void findById_whenEntityNull_returnsEmpty() {
        OrderId id = OrderId.generate();
        when(orderTable.getItem(any(Key.class))).thenReturn(null);

        StepVerifier.create(repository.findById(id))
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByEventId_returnsOrdersForEvent() {
        EventId eventId = EventId.generate();
        OrderEntity entity1 = buildEntity(OrderId.generate().value().toString());
        entity1.setEventId(eventId.value().toString());
        OrderEntity entity2 = buildEntity(OrderId.generate().value().toString());
        entity2.setEventId(eventId.value().toString());

        DynamoDbIndex<OrderEntity> index = mock(DynamoDbIndex.class);
        SdkIterable<Page<OrderEntity>> sdkIterable = mock(SdkIterable.class);
        Page<OrderEntity> page = mock(Page.class);

        when(orderTable.index("event-index")).thenReturn(index);
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(page));
        when(page.items()).thenReturn(List.of(entity1, entity2));

        StepVerifier.create(repository.findByEventId(eventId))
                .expectNextCount(2)
                .verifyComplete();
    }
}
