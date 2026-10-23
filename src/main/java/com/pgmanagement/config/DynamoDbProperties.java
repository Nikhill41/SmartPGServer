package com.pgmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for DynamoDB
 */
@Configuration
@ConfigurationProperties(prefix = "aws.dynamodb")
@Data
public class DynamoDbProperties {

    /**
     * DynamoDB table name for meter readings
     * Default: meter_readings
     */
    private String tableName = "meter_readings";
}
