package com.pgmanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for meter settings
 * Maps values from application.yml meter.* properties
 */
@Configuration
@ConfigurationProperties(prefix = "meter")
@Data
public class MeterProperties {

    /**
     * Threshold in seconds to determine if meter is online
     * Default: 60 seconds
     */
    private int onlineThresholdSeconds = 60;

    /**
     * Timeout for relay command confirmation in milliseconds
     * Default: 2000ms (2 seconds)
     */
    private int relayCommandTimeoutMs = 2000;

    /**
     * Number of retry attempts for relay command
     * Default: 3 retries
     */
    private int relayCommandRetryCount = 3;

    /**
     * Delay between retry attempts in milliseconds
     * Default: 500ms
     */
    private int relayCommandRetryDelayMs = 500;
}
