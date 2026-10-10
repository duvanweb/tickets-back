package com.tickets.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS SQS.
 *
 * @param region    the AWS region
 * @param endpoint  the SQS endpoint override (for local development via LocalStack; null in production)
 * @param queueName the name of the SQS queue to publish messages to
 */
@ConfigurationProperties(prefix = "aws.sqs")
public record AwsSqsProperties(
        String region,
        String endpoint,
        String queueName) {
}
