package com.tickets.infrastructure.config;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.usecase.event.CreateEventService;
import com.tickets.application.usecase.event.GetEventService;
import com.tickets.application.usecase.event.ListEventsService;
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
    public CreateEventUseCase createEventUseCase(EventRepository repository) {
        return new CreateEventService(repository);
    }

    @Bean
    public GetEventUseCase getEventUseCase(EventRepository repository) {
        return new GetEventService(repository);
    }

    @Bean
    public ListEventsUseCase listEventsUseCase(EventRepository repository) {
        return new ListEventsService(repository);
    }
}
