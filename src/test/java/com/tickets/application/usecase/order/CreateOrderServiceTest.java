package com.tickets.application.usecase.order;

import com.tickets.application.port.out.publishers.MessagePublisher;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private CreateOrderService service;

    @Test
    void create_withValidCommand_savesOrderAndPublishesMessage() {
        CreateOrderCommand command = new CreateOrderCommand(
                EventId.generate(), "customer-123", 3);

        Order savedOrder = Order.create(command.eventId(), command.customerId(), command.quantity());
        when(repository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(messagePublisher.publish(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(command))
                .assertNext(order -> {
                    org.assertj.core.api.Assertions.assertThat(order.eventId()).isEqualTo(command.eventId());
                    org.assertj.core.api.Assertions.assertThat(order.customerId()).isEqualTo("customer-123");
                    org.assertj.core.api.Assertions.assertThat(order.quantity()).isEqualTo(3);
                })
                .verifyComplete();

        verify(repository).save(any(Order.class));
        verify(messagePublisher).publish(anyString());
    }

    @Test
    void create_whenRepositoryFails_propagatesError() {
        CreateOrderCommand command = new CreateOrderCommand(
                EventId.generate(), "customer-123", 3);

        when(repository.save(any(Order.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(service.create(command))
                .verifyError(RuntimeException.class);

        verify(messagePublisher, never()).publish(anyString());
    }

    @Test
    void create_whenPublishFails_propagatesError() {
        CreateOrderCommand command = new CreateOrderCommand(
                EventId.generate(), "customer-123", 3);

        Order savedOrder = Order.create(command.eventId(), command.customerId(), command.quantity());
        when(repository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(messagePublisher.publish(anyString())).thenReturn(Mono.error(new RuntimeException("SQS error")));

        StepVerifier.create(service.create(command))
                .verifyError(RuntimeException.class);
    }
}
