package com.tickets.infrastructure.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;

import java.net.URI;

/**
 * Spring configuration for SQS beans.
 *
 * <p>Creates the {@link SqsClient} with an optional endpoint override for
 * local development (LocalStack). A {@link CommandLineRunner} ensures the
 * target queue exists at startup — but only when an endpoint override is
 * configured (local development). In production the queue must pre-exist.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsSqsProperties.class)
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(AwsSqsProperties props) {
        var builder = SqsClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    @Bean
    public CommandLineRunner createSqsQueue(SqsClient client, AwsSqsProperties props) {
        return args -> {
            if (props.endpoint() == null || props.endpoint().isBlank()) {
                return; // skip auto-creation in production
            }
            try {
                client.createQueue(CreateQueueRequest.builder()
                        .queueName(props.queueName())
                        .build());
            } catch (QueueNameExistsException ignored) {
                // Queue already exists — safe to ignore
            }
        };
    }
}
