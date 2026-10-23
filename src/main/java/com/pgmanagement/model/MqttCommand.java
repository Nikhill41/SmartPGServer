package com.pgmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Model for MQTT relay control command payload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttCommand {

    /**
     * Relay command: "ON" or "OFF"
     */
    private String relay;

    /**
     * Command timestamp in Unix milliseconds
     */
    private Long timestamp;

    /**
     * Unique command identifier for tracking
     */
    private String commandId;

    /**
     * Create a new relay command
     */
    public static MqttCommand createRelayCommand(String relayStatus) {
        return new MqttCommand(
                relayStatus.toUpperCase(),
                System.currentTimeMillis(),
                "CMD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
        );
    }
}
