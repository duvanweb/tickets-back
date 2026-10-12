package com.tickets.infrastructure.config;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.in.event.GetAvailabilityUseCase;
import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.port.in.order.ConfirmOrderUseCase;
import com.tickets.application.port.in.order.CreateOrderUseCase;
import com.tickets.application.port.in.order.GetOrderUseCase;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase;
import com.tickets.application.port.in.order.ListOrdersByEventUseCase;
import com.tickets.application.port.in.order.ProcessOrderUseCase;
import com.tickets.application.port.in.order.ReleaseExpiredReservationsUseCase;
import com.tickets.application.port.out.publishers.MessagePublisher;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.application.usecase.event.CreateEventService;
import com.tickets.application.usecase.event.GetAvailabilityService;
import com.tickets.application.usecase.event.GetEventService;
import com.tickets.application.usecase.event.ListEventsService;
import com.tickets.application.usecase.order.ConfirmOrderService;
import com.tickets.application.usecase.order.CreateOrderService;
import com.tickets.application.usecase.order.GetOrderService;
import com.tickets.application.usecase.order.GrantComplimentaryService;
import com.tickets.application.usecase.order.ListOrdersByEventService;
import com.tickets.application.usecase.order.ProcessOrderService;
import com.tickets.application.usecase.order.ReleaseExpiredReservationsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers use case implementations as Spring beans.
 *
 * <p>Use cases are NOT annotated with {@code @Service} — they are wired
 * here via {@code @Bean} to keep the application layer free of Spring
 * annotations (hexagonal architecture rule).</p>
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public CreateEventUseCase createEventUseCase(EventRepository repository,
                                                  TicketRepository ticketRepository) {
        return new CreateEventService(repository, ticketRepository);
    }

    @Bean
    public GetEventUseCase getEventUseCase(EventRepository repository) {
        return new GetEventService(repository);
    }

    @Bean
    public ListEventsUseCase listEventsUseCase(EventRepository repository) {
        return new ListEventsService(repository);
    }

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository,
                                                  EventRepository eventRepository,
                                                  MessagePublisher messagePublisher) {
        return new CreateOrderService(orderRepository, eventRepository, messagePublisher);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderService(orderRepository);
    }

    @Bean
    public ProcessOrderUseCase processOrderUseCase(OrderRepository orderRepository,
                                                    TicketRepository ticketRepository,
                                                    EventRepository eventRepository,
                                                    TicketTransitionRepository transitionRepository,
                                                    TicketReservationRepository ticketReservationRepository) {
        return new ProcessOrderService(orderRepository, ticketRepository, eventRepository,
                                        transitionRepository, ticketReservationRepository);
    }

    @Bean
    public ConfirmOrderUseCase confirmOrderUseCase(OrderRepository orderRepository,
                                                    TicketRepository ticketRepository,
                                                    EventRepository eventRepository,
                                                    TicketTransitionRepository transitionRepository) {
        return new ConfirmOrderService(orderRepository, ticketRepository, eventRepository, transitionRepository);
    }

    @Bean
    public ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase(
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            OrderRepository orderRepository,
            TicketTransitionRepository transitionRepository) {
        return new ReleaseExpiredReservationsService(ticketRepository, eventRepository, orderRepository, transitionRepository);
    }

    @Bean
    public GetAvailabilityUseCase getAvailabilityUseCase(EventRepository eventRepository) {
        return new GetAvailabilityService(eventRepository);
    }

    @Bean
    public GrantComplimentaryUseCase grantComplimentaryUseCase(
            TicketRepository ticketRepository,
            OrderRepository orderRepository,
            TicketTransitionRepository transitionRepository,
            TicketReservationRepository ticketReservationRepository) {
        return new GrantComplimentaryService(ticketRepository, orderRepository,
                transitionRepository, ticketReservationRepository);
    }

    @Bean
    public ListOrdersByEventUseCase listOrdersByEventUseCase(OrderRepository orderRepository,
                                                              EventRepository eventRepository) {
        return new ListOrdersByEventService(orderRepository, eventRepository);
    }
}
