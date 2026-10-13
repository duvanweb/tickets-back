package com.tickets.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;

import java.net.URI;

/**
 * Spring configuration for SQS beans.
 *
 * <p>Creates the {@link SqsClient} (shared by the API publisher and the
 * worker consumer) and ensures the queue exists at startup (for local
 * development with LocalStack). The {@code SqsMessageConsumer} and
 * {@code OrderCreatedHandler} are gated to the {@code worker} profile
 * separately. Queue creation only runs in the API (not the worker) to
 * avoid race conditions when both start simultaneously.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsSqsProperties.class)
public class SqsConfig {

    private static final Logger log = LoggerFactory.getLogger(SqsConfig.class);

    @Bean
    public SqsClient sqsClient(AwsSqsProperties props) {
        var builder = SqsClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    @Profile("!worker")
    public CommandLineRunner createQueue(SqsClient sqsClient, AwsSqsProperties props) {
        return args -> {
            if (props.queueName() == null || props.queueName().isBlank()) {
                return;
            }
            try {
                sqsClient.createQueue(CreateQueueRequest.builder()
                        .queueName(props.queueName())
                        .build());
                log.info("SQS queue '{}' created", props.queueName());
            } catch (QueueNameExistsException ignored) {
                log.debug("SQS queue '{}' already exists", props.queueName());
            } catch (Exception e) {
                log.warn("Failed to create SQS queue '{}': {}", props.queueName(), e.getMessage());
            }
        };
    }
}
