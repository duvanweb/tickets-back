package com.tickets.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS SQS.
 *
 * <p>Shared by the API (publishes messages via {@code SqsProducer}) and
 * the worker (consumes messages via {@code SqsMessageConsumer}).</p>
 *
 * @param region              the AWS region
 * @param endpoint            the SQS endpoint override (for LocalStack; null in production)
 * @param queueName           the name of the SQS queue (used by the publisher to resolve the URL)
 * @param queueUrl            the URL of the SQS queue (used by the consumer for polling)
 * @param maxNumberOfMessages the maximum number of messages to retrieve per poll (1-10)
 * @param waitTimeSeconds     the long polling wait time in seconds (0-20)
 * @param pollIntervalMs      the delay in milliseconds between poll cycles
 */
@ConfigurationProperties(prefix = "aws.sqs")
public record AwsSqsProperties(
        String region,
        String endpoint,
        String queueName,
        String queueUrl,
        int maxNumberOfMessages,
        int waitTimeSeconds,
        int pollIntervalMs) {
}
