package com.tickets.infrastructure.config;

import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
 * and {@link DynamoDbTable} beans. Table creation happens via a dedicated
 * {@code @Bean} method that runs during context initialization — before
 * the web server starts — to avoid a race condition where requests arrive
 * before tables exist.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsDynamoDbProperties.class)
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    /**
     * Marker bean indicating that all DynamoDB tables have been created.
     * Other beans depend on this to ensure tables exist before use.
     */
    public record TablesReady() {}

    @Bean
    public DynamoDbClient dynamoDbClient(AwsDynamoDbProperties props) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    /**
     * Creates all DynamoDB tables during context initialization, before the
     * web server starts accepting requests.
     */
    @Bean
    public TablesReady ensureTablesExist(DynamoDbClient client, AwsDynamoDbProperties props) {
        createTicketsTable(client, props);
        createOrdersTable(client, props);
        createEventsTable(client, props);
        createTransitionsTable(client, props);
        return new TablesReady();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public DynamoDbTable<EventEntity> eventTable(DynamoDbEnhancedClient enhancedClient,
                                                  AwsDynamoDbProperties props,
                                                  TablesReady tablesReady) {
        return enhancedClient.table(props.eventsTableName(), TableSchema.fromBean(EventEntity.class));
    }

    @Bean
    public DynamoDbTable<TicketEntity> ticketTable(DynamoDbEnhancedClient enhancedClient,
                                                   AwsDynamoDbProperties props,
                                                   TablesReady tablesReady) {
        return enhancedClient.table(props.tableName(), TableSchema.fromBean(TicketEntity.class));
    }

    @Bean
    public DynamoDbTable<OrderEntity> orderTable(DynamoDbEnhancedClient enhancedClient,
                                                  AwsDynamoDbProperties props,
                                                  TablesReady tablesReady) {
        return enhancedClient.table(props.ordersTableName(), TableSchema.fromBean(OrderEntity.class));
    }

    @Bean
    public DynamoDbTable<TicketTransitionEntity> transitionTable(DynamoDbEnhancedClient enhancedClient,
                                                                  AwsDynamoDbProperties props,
                                                                  TablesReady tablesReady) {
        return enhancedClient.table(props.transitionsTableName(),
                TableSchema.fromBean(TicketTransitionEntity.class));
    }

    private void createTicketsTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(props.tableName())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("ticketId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("ticketId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("eventId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("status")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("reservedUntil")
                                    .attributeType(ScalarAttributeType.S)
                                    .build())
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("event-status-index")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("eventId")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("status")
                                                    .keyType(KeyType.RANGE)
                                                    .build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .build(),
                            GlobalSecondaryIndex.builder()
                                    .indexName("status-reservedUntil-index")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("status")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("reservedUntil")
                                                    .keyType(KeyType.RANGE)
                                                    .build())
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("DynamoDB table '{}' created", props.tableName());
        } catch (ResourceInUseException ignored) {
            log.debug("DynamoDB table '{}' already exists", props.tableName());
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB table '{}': {}", props.tableName(), e.getMessage());
        }
    }

    private void createOrdersTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(props.ordersTableName())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("orderId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("orderId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("eventId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("event-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("eventId")
                                    .keyType(KeyType.HASH)
                                    .build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL)
                                    .build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("DynamoDB table '{}' created", props.ordersTableName());
        } catch (ResourceInUseException ignored) {
            log.debug("DynamoDB table '{}' already exists", props.ordersTableName());
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB table '{}': {}", props.ordersTableName(), e.getMessage());
        }
    }

    private void createEventsTable(DynamoDbClient client, AwsDynamoDbProperties props) {
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
            log.info("DynamoDB table '{}' created", props.eventsTableName());
        } catch (ResourceInUseException ignored) {
            log.debug("DynamoDB table '{}' already exists", props.eventsTableName());
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB table '{}': {}", props.eventsTableName(), e.getMessage());
        }
    }

    private void createTransitionsTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        try {
            client.createTable(CreateTableRequest.builder()
                    .tableName(props.transitionsTableName())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("transitionId")
                            .keyType(KeyType.HASH)
                            .build())
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("transitionId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("ticketId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("timestamp")
                                    .attributeType(ScalarAttributeType.S)
                                    .build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("ticket-id-index")
                            .keySchema(
                                    KeySchemaElement.builder()
                                            .attributeName("ticketId")
                                            .keyType(KeyType.HASH)
                                            .build(),
                                    KeySchemaElement.builder()
                                            .attributeName("timestamp")
                                            .keyType(KeyType.RANGE)
                                            .build())
                            .projection(Projection.builder()
                                    .projectionType(ProjectionType.ALL)
                                    .build())
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            log.info("DynamoDB table '{}' created", props.transitionsTableName());
        } catch (ResourceInUseException ignored) {
            log.debug("DynamoDB table '{}' already exists", props.transitionsTableName());
        } catch (Exception e) {
            log.warn("Failed to create DynamoDB table '{}': {}", props.transitionsTableName(), e.getMessage());
        }
    }
}
