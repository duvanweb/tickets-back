package com.tickets.infrastructure.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;

import java.net.URI;

/**
 * Spring configuration for SQS beans.
 *
 * <p>Only active under the {@code worker} profile. Creates the
 * {@link SqsClient} and ensures the queue exists at startup
 * (for local development with LocalStack).</p>
 */
@Configuration
@Profile("worker")
@EnableConfigurationProperties(SqsProperties.class)
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(SqsProperties props) {
        var builder = SqsClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    @Bean
    public CommandLineRunner createQueue(SqsClient sqsClient, SqsProperties props) {
        return args -> {
            var queueName = extractQueueName(props.queueUrl());
            try {
                sqsClient.createQueue(CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build());
            } catch (QueueNameExistsException ignored) {
                // Queue already exists — safe to ignore
            }
        };
    }

    private String extractQueueName(String queueUrl) {
        var parts = queueUrl.split("/");
        return parts[parts.length - 1];
    }
}
