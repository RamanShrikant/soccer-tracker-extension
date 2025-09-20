package com.raman.soccer_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        try {
            System.out.println("⚡ Creating DynamoDbClient bean...");
            return DynamoDbClient.builder()
                    .region(Region.US_EAST_2) // match your table’s region
                    .build();
        } catch (Exception e) {
            System.err.println("❌ Failed to create DynamoDbClient: " + e.getMessage());
            throw e;
        }
    }
}
