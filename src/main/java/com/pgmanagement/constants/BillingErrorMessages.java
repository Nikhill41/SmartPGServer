package com.pgmanagement.constants;



public class BillingErrorMessages {

    // Configuration Errors
    public static final String CONFIG_NOT_FOUND = "Billing configuration not found for PG ID: %d";
    public static final String CONFIG_ALREADY_EXISTS = "Billing configuration already exists for PG ID: %d";

    // Standard Charge Errors
    public static final String CHARGE_NOT_FOUND = "Standard charge not found with ID: %d";
    public static final String CHARGE_NAME_DUPLICATE = "Standard charge with name '%s' already exists for this PG";
    public static final String CHARGE_IN_USE = "Cannot delete charge that is being used in active bills";

    // Bill Errors
    public static final String BILL_NOT_FOUND = "Bill not found with ID: %d";
    public static final String BILL_NUMBER_DUPLICATE = "Bill number already exists: %s";
    public static final String BILL_ALREADY_PAID = "Cannot modify bill that is already paid";
    public static final String BILL_ALREADY_CANCELLED = "Bill is already cancelled";
    public static final String INVALID_BILL_STATUS_TRANSITION = "Cannot change bill status from %s to %s";

    // Payment Proof Errors
    public static final String PAYMENT_PROOF_NOT_UPLOADED = "No payment proof uploaded for bill ID: %d";
    public static final String PAYMENT_PROOF_ALREADY_VERIFIED = "Payment proof already verified";
    public static final String MAX_REJECTION_ATTEMPTS_REACHED = "Maximum payment rejection attempts reached. Please contact owner.";
    public static final String PAYMENT_PROOF_UPLOAD_FAILED = "Failed to upload payment proof: %s";

    // Tenant Stay Errors
    public static final String STAY_RECORD_NOT_FOUND = "Stay record not found for tenant ID: %d";
    public static final String NO_ACTIVE_STAY = "No active stay found for tenant";
    public static final String STAY_ALREADY_COMPLETED = "Stay record is already completed";
    public static final String CHECKOUT_ALREADY_INITIATED = "Checkout already initiated for this tenant";

    // Billing Period Errors
    public static final String INVALID_BILLING_PERIOD = "Billing period end date must be after start date";
    public static final String BILLING_PERIOD_IN_FUTURE = "Cannot generate bill for future period";
    public static final String BILL_ALREADY_EXISTS_FOR_PERIOD = "Bill already exists for period %s to %s";

    // Meter Errors
    public static final String ROOM_NO_METER = "Room ID %d does not have a paired meter";
    public static final String METER_OFFLINE = "Meter %s is offline. Last reading: %s";
    public static final String METER_DATA_UNAVAILABLE = "Meter reading data unavailable for the billing period";

    // Authorization Errors
    public static final String NOT_BILL_OWNER = "You are not authorized to access this bill";
    public static final String NOT_BILL_TENANT = "This bill does not belong to you";
    public static final String NOT_PG_OWNER = "You do not own this PG";

    // Validation Errors
    public static final String INVALID_DUE_DATE = "Due date must be after bill generation date";
    public static final String INVALID_AUTO_GENERATION_DAY = "Auto generation day must be between 1 and 28";
    public static final String INVALID_GST_PERCENTAGE = "GST percentage must be between 0 and 100";
    public static final String NEGATIVE_AMOUNT = "Amount cannot be negative";

    // Checkout Errors
    public static final String PENDING_BILLS_EXIST = "Cannot complete checkout. %d pending bills found";
    public static final String SECURITY_DEPOSIT_NOT_PAID = "Security deposit must be paid before checkout";
    public static final String CHECKOUT_DATE_INVALID = "Checkout date cannot be in the past";

    // Auto-Billing Errors
    public static final String AUTO_BILLING_DISABLED = "Auto-billing is disabled for this PG";
    public static final String NO_ACTIVE_TENANTS = "No active tenants found for billing";
    public static final String SECURITY_DEPOSIT_EXISTS = "Security Deposit Alrady Exists";

    private BillingErrorMessages() {
        // Private constructor to prevent instantiation
    }
}

