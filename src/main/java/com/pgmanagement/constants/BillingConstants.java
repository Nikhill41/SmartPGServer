package com.pgmanagement.constants;




public class BillingConstants {

    // Bill Number Formats
    public static final String BILL_NUMBER_FORMAT = "%s-%s-%s-%d";  // PG-ROOM-YYYY-MM-ID
    public static final String SECURITY_DEPOSIT_PREFIX = "SD";
    public static final String FINAL_SETTLEMENT_PREFIX = "FS";
    public static final String MONTHLY_BILL_PREFIX = "MB";

    // Default Values
    public static final double DEFAULT_ELECTRICITY_RATE = 8.5;
    public static final int DEFAULT_SECURITY_DEPOSIT = 10000;
    public static final int DEFAULT_DUE_DAYS_OFFSET = 7;
    public static final int DEFAULT_SECURITY_DEPOSIT_DUE_DAYS = 7;
    public static final int DEFAULT_LATE_FEE_GRACE_DAYS = 3;
    public static final double DEFAULT_LATE_FEE_AMOUNT = 200.0;

    // Billing Configuration Constraints
    public static final int MIN_AUTO_GENERATION_DAY = 1;
    public static final int MAX_AUTO_GENERATION_DAY = 28;
    public static final int MIN_DUE_DAYS_OFFSET = 1;
    public static final int MAX_DUE_DAYS_OFFSET = 30;
    public static final double MIN_GST_PERCENTAGE = 0.0;
    public static final double MAX_GST_PERCENTAGE = 100.0;

    // Payment Proof
    public static final int MAX_PAYMENT_REJECTION_ATTEMPTS = 5;
    public static final String PAYMENT_PROOF_FOLDER = "payment-proofs";

    // Bill Generation
    public static final int MIN_PARTIAL_BILLING_DAYS = 7;  // Minimum days for pro-rated billing

    // Meter Online Threshold
    public static final long METER_ONLINE_THRESHOLD_SECONDS = 60;

    // Validation Messages
    public static final String MSG_INVALID_BILLING_PERIOD = "Billing period must be in the past";
    public static final String MSG_INVALID_DUE_DATE = "Due date must be after generation date";
    public static final String MSG_BILL_ALREADY_PAID = "Cannot modify a paid bill";
    public static final String MSG_NO_ACTIVE_STAY = "No active stay record found for tenant";
    public static final String MSG_ROOM_HAS_NO_METER = "Room does not have a paired meter";
    public static final String MSG_INVALID_GST_PERCENTAGE = "GST percentage must be between 0 and 100";
    public static final String MSG_SECURITY_DEPOSIT_EXISTS = "Security deposit bill already exists for this tenant";

    // UPI Payment Link Format
    public static final String UPI_PAYMENT_LINK_FORMAT = "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s";

    // Email Subjects
    public static final String EMAIL_SUBJECT_SECURITY_DEPOSIT = "Security Deposit Required - %s";
    public static final String EMAIL_SUBJECT_MONTHLY_BILL = "Your Bill for %s - %s";
    public static final String EMAIL_SUBJECT_PAYMENT_VERIFIED = "Payment Confirmed - Bill #%s";
    public static final String EMAIL_SUBJECT_PAYMENT_REJECTED = "Payment Proof Rejected - Bill #%s";
    public static final String EMAIL_SUBJECT_BILL_UPDATED = "Updated Bill - Bill #%s";
    public static final String EMAIL_SUBJECT_OVERDUE = "Overdue Notice - Bill #%s";
    public static final String EMAIL_SUBJECT_LATE_FEE = "Late Fee Applied - Bill #%s";
    public static final String EMAIL_SUBJECT_FINAL_SETTLEMENT = "Final Settlement - Checkout from %s";
    public static final String EMAIL_SUBJECT_CHECKOUT_COMPLETE = "Checkout Complete - Thank You!";

    private BillingConstants() {
        // Private constructor to prevent instantiation
    }
}

