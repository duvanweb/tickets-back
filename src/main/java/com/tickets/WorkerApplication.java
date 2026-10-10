package com.tickets;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Tickets Worker application.
 *
 * <p>Consumes messages from AWS SQS without starting a web server.
 * The {@code worker} profile activates SQS-specific beans only.
 * Scheduling is enabled for the SQS polling loop.</p>
 */
@SpringBootApplication
@EnableScheduling
public class WorkerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(WorkerApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("worker")
                .run(args);
    }
}
