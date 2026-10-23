package com.pgmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for relay control command
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelayCommandRequest {

    /**
     * Relay command: "ON" or "OFF"
     */
    @NotBlank(message = "Command is required")
    @Pattern(regexp = "^(ON|OFF)$", message = "Command must be either 'ON' or 'OFF'")
    private String command;
}
