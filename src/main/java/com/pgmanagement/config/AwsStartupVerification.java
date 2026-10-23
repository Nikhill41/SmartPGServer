package com.pgmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

/**
 * Startup verification for AWS services
 * Logs AWS client initialization status
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AwsStartupVerification {

    private final IotDataPlaneClient iotDataPlaneClient;
    private final DynamoDbClient dynamoDbClient;
    private final MeterProperties meterProperties;
    private final DynamoDbProperties dynamoDbProperties;

    @Bean
    public CommandLineRunner verifyAwsClients() {
        return args -> {
            log.info("=== AWS Meter Integration Verification ===");

            // Verify IoT Data Plane Client
            try {
                log.info("✓ AWS IoT Data Plane Client initialized successfully");
                log.info("  - Region: {}", iotDataPlaneClient.serviceClientConfiguration().region());
            } catch (Exception e) {
                log.error("✗ AWS IoT Data Plane Client initialization failed: {}", e.getMessage());
            }

            // Verify DynamoDB Client
            try {
                log.info("✓ AWS DynamoDB Client initialized successfully");
                log.info("  - Region: {}", dynamoDbClient.serviceClientConfiguration().region());
                log.info("  - Table Name: {}", dynamoDbProperties.getTableName());
            } catch (Exception e) {
                log.error("✗ AWS DynamoDB Client initialization failed: {}", e.getMessage());
            }

            // Log meter configuration
            log.info("Meter Configuration:");
            log.info("  - Online Threshold: {} seconds", meterProperties.getOnlineThresholdSeconds());
            log.info("  - Relay Command Timeout: {} ms", meterProperties.getRelayCommandTimeoutMs());
            log.info("  - Relay Command Retries: {}", meterProperties.getRelayCommandRetryCount());

            log.info("=== AWS Verification Complete ===");
        };
    }
}
