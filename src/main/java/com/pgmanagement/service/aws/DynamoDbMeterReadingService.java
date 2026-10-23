package com.pgmanagement.service.aws;

import com.pgmanagement.config.DynamoDbProperties;
import com.pgmanagement.model.MeterReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for querying meter readings from DynamoDB
 * Handles time-series data retrieval and processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamoDbMeterReadingService {

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbProperties dynamoDbProperties;

    /**
     * Query meter readings within a time range
     *
     * @param meterId Meter identifier (partition key)
     * @param startTimestamp Start time in Unix milliseconds
     * @param endTimestamp End time in Unix milliseconds
     * @return List of meter readings sorted by timestamp
     */
    public List<MeterReading> queryReadings(String meterId, long startTimestamp, long endTimestamp) {
        log.info("Querying DynamoDB for meter: {}, range: {} to {}", meterId, startTimestamp, endTimestamp);

        try {
            // Build query request
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":meterId", AttributeValue.builder().s(meterId).build());
            expressionValues.put(":start", AttributeValue.builder().n(String.valueOf(startTimestamp)).build());
            expressionValues.put(":end", AttributeValue.builder().n(String.valueOf(endTimestamp)).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#ts", "timestamp");

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(dynamoDbProperties.getTableName())
                    .keyConditionExpression("meterId = :meterId AND #ts BETWEEN :start AND :end")
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .scanIndexForward(true) // Sort ascending by timestamp
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);

            List<MeterReading> readings = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                MeterReading reading = mapItemToReading(item);
                readings.add(reading);
            }

            log.info("Retrieved {} readings for meter {}", readings.size(), meterId);

            return readings;

        } catch (Exception e) {
            log.error("Error querying DynamoDB for meter {}: {}", meterId, e.getMessage(), e);
            throw new RuntimeException("Failed to query meter readings: " + e.getMessage(), e);
        }
    }

    /**
     * Get latest N readings for a meter
     *
     * @param meterId Meter identifier
     * @param limit Maximum number of readings to retrieve
     * @return List of recent readings
     */
    public List<MeterReading> getLatestReadings(String meterId, int limit) {
        log.info("Querying latest {} readings for meter: {}", limit, meterId);

        try {
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":meterId", AttributeValue.builder().s(meterId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(dynamoDbProperties.getTableName())
                    .keyConditionExpression("meterId = :meterId")
                    .expressionAttributeValues(expressionValues)
                    .scanIndexForward(false) // Sort descending (latest first)
                    .limit(limit)
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);

            List<MeterReading> readings = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                readings.add(mapItemToReading(item));
            }

            log.info("Retrieved {} latest readings for meter {}", readings.size(), meterId);

            return readings;

        } catch (Exception e) {
            log.error("Error querying latest readings for meter {}: {}", meterId, e.getMessage(), e);
            throw new RuntimeException("Failed to query latest readings: " + e.getMessage(), e);
        }
    }

    /**
     * Count total readings for a meter within time range
     *
     * @param meterId Meter identifier
     * @param startTimestamp Start time
     * @param endTimestamp End time
     * @return Count of readings
     */
    public int countReadings(String meterId, long startTimestamp, long endTimestamp) {
        log.info("Counting readings for meter: {} in range {} to {}", meterId, startTimestamp, endTimestamp);

        try {
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":meterId", AttributeValue.builder().s(meterId).build());
            expressionValues.put(":start", AttributeValue.builder().n(String.valueOf(startTimestamp)).build());
            expressionValues.put(":end", AttributeValue.builder().n(String.valueOf(endTimestamp)).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#ts", "timestamp");

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(dynamoDbProperties.getTableName())
                    .keyConditionExpression("meterId = :meterId AND #ts BETWEEN :start AND :end")
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .select(Select.COUNT)
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);
            int count = response.count();

            log.info("Found {} readings for meter {}", count, meterId);

            return count;

        } catch (Exception e) {
            log.error("Error counting readings for meter {}: {}", meterId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Map DynamoDB item to MeterReading object
     *
     * @param item DynamoDB item
     * @return MeterReading object
     */
    private MeterReading mapItemToReading(Map<String, AttributeValue> item) {
        MeterReading reading = new MeterReading();

        reading.setMeterId(getStringValue(item, "meterId"));
        reading.setTimestamp(getLongValue(item, "timestamp"));
        reading.setVoltage(getDoubleValue(item, "voltage"));
        reading.setCurrent(getDoubleValue(item, "current"));
        reading.setPower(getDoubleValue(item, "power"));
        reading.setEnergy(getDoubleValue(item, "energy"));
        reading.setFrequency(getDoubleValue(item, "frequency"));
        reading.setPowerFactor(getDoubleValue(item, "powerFactor"));
        reading.setRelayStatus(getStringValue(item, "relayStatus"));

        return reading;
    }

    /**
     * Helper: Extract string value from DynamoDB item
     */
    private String getStringValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value != null && value.s() != null ? value.s() : null;
    }

    /**
     * Helper: Extract long value from DynamoDB item
     */
    private Long getLongValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value != null && value.n() != null) {
            try {
                return Long.parseLong(value.n());
            } catch (NumberFormatException e) {
                log.warn("Invalid long value for key {}: {}", key, value.n());
            }
        }
        return null;
    }

    /**
     * Helper: Extract double value from DynamoDB item
     */
    private Double getDoubleValue(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        if (value != null && value.n() != null) {
            try {
                return Double.parseDouble(value.n());
            } catch (NumberFormatException e) {
                log.warn("Invalid double value for key {}: {}", key, value.n());
            }
        }
        return null;
    }

    public double getMeterReadingAtDate(String meterId, LocalDateTime dateTime) {

        long targetTimestamp = dateTime
                .atZone(ZoneOffset.UTC)   // IMPORTANT: use UTC
                .toEpochSecond();         // IMPORTANT: seconds, not millis

        log.debug("Fetching meter reading for meter {} at {} (epochSeconds={})",
                meterId, dateTime, targetTimestamp);

        try {
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":meterId", AttributeValue.builder().s(meterId).build());
            expressionValues.put(":target", AttributeValue.builder().n(String.valueOf(targetTimestamp)).build());

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#ts", "timestamp");

            // -------- 1️⃣ Try reading BEFORE or equal to target --------
            QueryRequest beforeQuery = QueryRequest.builder()
                    .tableName(dynamoDbProperties.getTableName())
                    .keyConditionExpression("meterId = :meterId AND #ts <= :target")
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .scanIndexForward(false) // descending order
                    .limit(1)
                    .build();

            QueryResponse beforeResponse = dynamoDbClient.query(beforeQuery);

            if (!beforeResponse.items().isEmpty()) {
                MeterReading reading = mapItemToReading(beforeResponse.items().get(0));
                return reading.getEnergy() != null ? reading.getEnergy() : 0.0;
            }

            // -------- 2️⃣ Fallback: first reading AFTER target --------
            QueryRequest afterQuery = QueryRequest.builder()
                    .tableName(dynamoDbProperties.getTableName())
                    .keyConditionExpression("meterId = :meterId AND #ts >= :target")
                    .expressionAttributeValues(expressionValues)
                    .expressionAttributeNames(expressionNames)
                    .scanIndexForward(true) // ascending order
                    .limit(1)
                    .build();

            QueryResponse afterResponse = dynamoDbClient.query(afterQuery);

            if (!afterResponse.items().isEmpty()) {
                MeterReading reading = mapItemToReading(afterResponse.items().get(0));
                return reading.getEnergy() != null ? reading.getEnergy() : 0.0;
            }

            // -------- 3️⃣ No readings exist --------
            log.warn("No meter readings found for meter {}", meterId);
            return 0.0;

        } catch (Exception e) {
            log.error("Error fetching meter reading for meter {}: {}", meterId, e.getMessage(), e);
            return 0.0;
        }
    }

}
