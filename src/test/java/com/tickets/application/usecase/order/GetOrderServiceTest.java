package com.tickets.application.usecase.order;

import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.exception.OrderNotFoundException;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock
    private OrderRepository repository;

    @InjectMocks
    private GetOrderService service;

    @Test
    void getById_whenOrderExists_returnsOrder() {
        OrderId id = OrderId.generate();
        Order order = Order.create(EventId.generate(), "customer-123", 3);
        Order reconstituted = Order.reconstitute(id, order.eventId(), order.customerId(),
                order.quantity(), order.status(), order.createdAt(), order.reservedUntil(), order.ticketIds());

        when(repository.findById(id)).thenReturn(Mono.just(reconstituted));

        StepVerifier.create(service.getById(id))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.id()).isEqualTo(id);
                    org.assertj.core.api.Assertions.assertThat(result.customerId()).isEqualTo("customer-123");
                })
                .verifyComplete();
    }

    @Test
    void getById_whenOrderDoesNotExist_throwsOrderNotFoundException() {
        OrderId id = OrderId.generate();

        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(id))
                .verifyError(OrderNotFoundException.class);
    }
}
