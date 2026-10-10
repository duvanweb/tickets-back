package com.tickets.infrastructure.config;

import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

/**
 * Spring configuration for DynamoDB beans.
 *
 * <p>Creates the {@link DynamoDbClient}, {@link DynamoDbEnhancedClient},
 * and {@link DynamoDbTable} for events. A {@link CommandLineRunner} ensures
 * the events table exists at startup (for local development).</p>
 */
@Configuration
@EnableConfigurationProperties(AwsDynamoDbProperties.class)
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AwsDynamoDbProperties props) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public DynamoDbTable<EventEntity> eventTable(DynamoDbEnhancedClient enhancedClient,
                                                  AwsDynamoDbProperties props) {
        return enhancedClient.table(props.eventsTableName(), TableSchema.fromBean(EventEntity.class));
    }

    @Bean
    public CommandLineRunner createEventTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        return args -> {
            try {
                client.createTable(CreateTableRequest.builder()
                        .tableName(props.eventsTableName())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("eventId")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("eventId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("name")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                .indexName("name-index")
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("name")
                                        .keyType(KeyType.HASH)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
            } catch (ResourceInUseException ignored) {
                // Table already exists — safe to ignore
            }
        };
    }
}
