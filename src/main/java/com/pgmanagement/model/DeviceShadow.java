package com.pgmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Model for parsing AWS IoT Device Shadow JSON response
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceShadow {

    private State state;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class State {
        private Reported reported;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reported {
        private Double voltage;
        private Double current;
        private Double power;
        private Double energy;
        private Double frequency;

        @JsonProperty("powerFactor")
        private Double powerFactor;

        @JsonProperty("relayStatus")
        private String relayStatus;

        private Long timestamp;
    }

    /**
     * Helper method to get reported state directly
     */
    public Reported getReportedState() {
        return state != null && state.reported != null ? state.reported : null;
    }

    /**
     * Check if shadow has valid reported data
     */
    public boolean hasReportedData() {
        return getReportedState() != null && getReportedState().getTimestamp() != null;
    }
}
