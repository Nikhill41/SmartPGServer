package com.pgmanagement.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for formatting timestamps into human-readable strings
 */
public class TimeFormatter {

    /**
     * Format epoch milliseconds to "X seconds/minutes/hours/days ago"
     *
     * @param timestampMs Unix timestamp in milliseconds
     * @return Human-readable time difference string
     */
    public static String formatTimeAgo(long timestampMs) {
        long now = System.currentTimeMillis();
        long diffMs = now - timestampMs;

        if (diffMs < 0) {
            return "just now";
        }

        long seconds = diffMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return seconds + (seconds == 1 ? " second ago" : " seconds ago");
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    /**
     * Format LocalDateTime to "X seconds/minutes/hours/days ago"
     */
    public static String formatTimeAgo(Long deviceTimestamp) {
        if (deviceTimestamp == null) {
            return "unknown";
        }

        // ✅ Convert device timestamp (seconds) to Instant
        Instant deviceTime = Instant.ofEpochSecond(deviceTimestamp);
        Instant now = Instant.now();

        Duration duration = Duration.between(deviceTime, now);
        long seconds = duration.getSeconds();

        if (seconds < 0) {
            return "in the future"; // Device time ahead of server
        } else if (seconds < 60) {
            return seconds + " seconds ago";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            long days = seconds / 86400;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }

    /**
     * Convert epoch milliseconds to LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestampMs) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs),
                ZoneId.systemDefault()
        );
    }

    /**
     * Convert LocalDateTime to epoch milliseconds
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Check if timestamp is within last N seconds
     */
    public static boolean isWithinLastSeconds(Long deviceTimestamp, int thresholdSeconds) {
        if (deviceTimestamp == null) {
            return false;
        }

        // ✅ Get current time in SECONDS
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // ✅ Calculate difference
        long differenceSeconds = currentTimeSeconds - deviceTimestamp;

        // ✅ Check if within threshold
        boolean isWithin = differenceSeconds >= 0 && differenceSeconds <= thresholdSeconds;

        // Debug logging
        System.out.println("Current time (seconds): " + currentTimeSeconds);
        System.out.println("Device timestamp (seconds): " + deviceTimestamp);
        System.out.println("Difference: " + differenceSeconds + " seconds");
        System.out.println("Threshold: " + thresholdSeconds + " seconds");
        System.out.println("Is within threshold: " + isWithin);

        return isWithin;
    }

    /**
     * Calculate time difference in seconds
     */
    public static long getDifferenceInSeconds(long timestampMs) {
        long now = System.currentTimeMillis();
        return (now - timestampMs) / 1000;
    }
}
