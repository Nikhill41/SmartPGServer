package com.pgmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;

/**
 * AWS Configuration for IoT Core and DynamoDB
 * Configures AWS SDK v2 clients for meter integration
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.access-key}")
    private String awsAccessKey;

    @Value("${aws.secret-key}")
    private String awsSecretKey;

    @Value("${aws.iot.endpoint}")
    private String iotEndpoint;

    /**
     * AWS IoT Data Plane Client for Device Shadow and MQTT operations
     * Used for:
     * - Getting device shadow (live meter readings)
     * - Publishing MQTT commands (relay control)
     */
    @Bean
    public IotDataPlaneClient iotDataPlaneClient() {

        if (iotEndpoint == null || !iotEndpoint.startsWith("https://")) {
            throw new IllegalStateException(
                    "AWS IoT endpoint must start with https:// but was: " + iotEndpoint
            );
        }

        return IotDataPlaneClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                        )
                )
                .endpointOverride(java.net.URI.create(iotEndpoint))
                .build();
    }


    /**
     * AWS DynamoDB Client for time-series meter readings
     * Used for querying historical meter data from meter_readings table
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                ))
                .build();
    }
}
