package com.pgmanagement.service.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgmanagement.config.MeterProperties;
import com.pgmanagement.model.DeviceShadow;
import com.pgmanagement.model.MqttCommand;
import com.pgmanagement.util.TimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.*;

import java.nio.charset.StandardCharsets;

/**
 * Service for AWS IoT Device Shadow operations
 * Handles communication with AWS IoT Core for meter data and control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsIotShadowService {

    private final IotDataPlaneClient iotDataPlaneClient;
    private final MeterProperties meterProperties;
    private final ObjectMapper objectMapper;

    /**
     * Get device shadow for a meter
     *
     * @param thingName AWS IoT Thing Name (same as meterId)
     * @return DeviceShadow object with reported state
     * @throws IotDataPlaneException if shadow fetch fails
     */
    public DeviceShadow getDeviceShadow(String thingName) {
        log.info("Fetching device shadow for thing: {}", thingName);

        try {
            log.info("Inside the block1 : {}", thingName);
            GetThingShadowRequest request = GetThingShadowRequest.builder()
                    .thingName(thingName)
                    .build();
            log.info("Inside the block2 : {}", thingName);

            GetThingShadowResponse response = iotDataPlaneClient.getThingShadow(request);

            // Parse shadow JSON from response
            String shadowJson = response.payload().asUtf8String();
            log.debug("Shadow JSON for {}: {}", thingName, shadowJson);
            log.info("Inside the block3 : {}", thingName);

            DeviceShadow shadow = objectMapper.readValue(shadowJson, DeviceShadow.class);

            if (!shadow.hasReportedData()) {
                log.warn("Shadow for {} has no reported data", thingName);
            }

            return shadow;

        } catch (ResourceNotFoundException e) {
            log.error("Thing shadow not found for: {}", thingName);
            throw new RuntimeException("Device shadow not found for meter: " + thingName, e);
        } catch (Exception e) {
            log.error("Error fetching shadow for {}: {}", thingName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch device shadow: " + e.getMessage(), e);
        }
    }

    /**
     * Check if meter is online based on shadow timestamp
     *
     * @param shadow Device shadow
     * @return true if meter is online (last reading within threshold)
     */
    public boolean isMeterOnline(DeviceShadow shadow) {
        if (!shadow.hasReportedData()) {
            return false;
        }

        Long timestamp = shadow.getReportedState().getTimestamp();
        if (timestamp == null) {
            return false;
        }

        boolean isOnline = TimeFormatter.isWithinLastSeconds(
                timestamp,
                meterProperties.getOnlineThresholdSeconds()
        );

        log.debug("Meter online check: timestamp={}, threshold={}s, online={}",
                timestamp, meterProperties.getOnlineThresholdSeconds(), isOnline);

        return isOnline;
    }

    /**
     * Get time since last reading in human-readable format
     *
     * @param shadow Device shadow
     * @return "X seconds/minutes ago" string
     */
    public String getLastSeenDisplay(DeviceShadow shadow) {
        if (!shadow.hasReportedData()) {
            return "never";
        }

        Long timestamp = shadow.getReportedState().getTimestamp();
        if (timestamp == null) {
            return "unknown";
        }

        return TimeFormatter.formatTimeAgo(timestamp);
    }

    /**
     * Publish relay control command to meter via MQTT
     *
     * @param meterId Meter ID (used in topic)
     * @param relayCommand "ON" or "OFF"
     * @return Command ID for tracking
     */
    public String publishRelayCommand(String meterId, String relayCommand) {
        log.info("Publishing relay command to meter {}: {}", meterId, relayCommand);

        try {
            // Create command payload
            MqttCommand command = MqttCommand.createRelayCommand(relayCommand);
            String payloadJson = objectMapper.writeValueAsString(command);

            log.debug("Command payload: {}", payloadJson);

            // Publish to MQTT topic: pg/meter/{meterId}/command
            String topic = String.format("pg/meter/%s/command", meterId);

            PublishRequest request = PublishRequest.builder()
                    .topic(topic)
                    .qos(1) // QoS 1: At least once delivery
                    .payload(SdkBytes.fromUtf8String(payloadJson))
                    .build();

            PublishResponse response = iotDataPlaneClient.publish(request);

            log.info("Relay command published successfully to topic: {}, commandId: {}",
                    topic, command.getCommandId());

            return command.getCommandId();

        } catch (Exception e) {
            log.error("Error publishing relay command for {}: {}", meterId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish relay command: " + e.getMessage(), e);
        }
    }

    /**
     * Poll shadow to verify relay status change
     * Retries multiple times with delay
     *
     * @param thingName AWS IoT Thing Name
     * @param expectedStatus Expected relay status ("ON" or "OFF")
     * @return true if relay status matches expected value
     */
    public boolean verifyRelayStatus(String thingName, String expectedStatus) {
        log.info("Verifying relay status for {}: expecting {}", thingName, expectedStatus);

        int retryCount = meterProperties.getRelayCommandRetryCount();
        int retryDelay = meterProperties.getRelayCommandRetryDelayMs();

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                // Wait before checking
                if (attempt > 1) {
                    Thread.sleep(retryDelay);
                }

                DeviceShadow shadow = getDeviceShadow(thingName);

                if (shadow.hasReportedData()) {
                    String actualStatus = shadow.getReportedState().getRelayStatus();

                    if (expectedStatus.equalsIgnoreCase(actualStatus)) {
                        log.info("Relay status verified successfully on attempt {}/{}", attempt, retryCount);
                        return true;
                    } else {
                        log.debug("Relay status mismatch on attempt {}/{}: expected={}, actual={}",
                                attempt, retryCount, expectedStatus, actualStatus);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Verification interrupted", e);
                return false;
            } catch (Exception e) {
                log.warn("Error during verification attempt {}/{}: {}", attempt, retryCount, e.getMessage());
            }
        }

        log.warn("Relay status verification failed after {} attempts", retryCount);
        return false;
    }

    /**
     * Get last reading timestamp from shadow
     *
     * @param shadow Device shadow
     * @return Timestamp in milliseconds, or null if not available
     */
    public Long getLastReadingTimestamp(DeviceShadow shadow) {
        if (!shadow.hasReportedData()) {
            return null;
        }
        return shadow.getReportedState().getTimestamp();
    }
}
