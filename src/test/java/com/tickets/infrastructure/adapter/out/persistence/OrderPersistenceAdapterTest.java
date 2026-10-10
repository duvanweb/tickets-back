package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceAdapterTest {

    @Mock
    private OrderDynamoDbRepository dynamoDbRepository;

    @InjectMocks
    private OrderPersistenceAdapter adapter;

    @Test
    void save_delegatesToDynamoDb() {
        Order order = Order.create(EventId.generate(), "customer-123", 3);
        when(dynamoDbRepository.save(order)).thenReturn(Mono.just(order));

        StepVerifier.create(adapter.save(order))
                .expectNext(order)
                .verifyComplete();

        verify(dynamoDbRepository).save(order);
    }

    @Test
    void findById_delegatesToDynamoDb() {
        OrderId id = OrderId.generate();
        Order order = Order.create(EventId.generate(), "customer-123", 3);
        when(dynamoDbRepository.findById(id)).thenReturn(Mono.just(order));

        StepVerifier.create(adapter.findById(id))
                .expectNext(order)
                .verifyComplete();

        verify(dynamoDbRepository).findById(id);
    }
}
