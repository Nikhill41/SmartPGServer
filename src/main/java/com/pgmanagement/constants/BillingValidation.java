package com.pgmanagement.constants;



public class BillingValidation {

    // Standard Charge Validation
    public static final int CHARGE_NAME_MIN_LENGTH = 2;
    public static final int CHARGE_NAME_MAX_LENGTH = 100;
    public static final String CHARGE_NAME_PATTERN = "^[a-zA-Z0-9\\s-]+$";

    // Amount Validation
    public static final double MIN_AMOUNT = 0.01;
    public static final double MAX_AMOUNT = 999999.99;
    public static final double MIN_RENT = 100.0;
    public static final double MAX_RENT = 1000000.0;

    // Bill Number Validation
    public static final String BILL_NUMBER_PATTERN = "^[A-Z]+-[0-9]+-[0-9]{4}-[0-9]{2}-[0-9]+$";
    public static final int BILL_NUMBER_MAX_LENGTH = 50;

    // Notes and Reasons
    public static final int NOTES_MAX_LENGTH = 1000;
    public static final int REJECTION_REASON_MIN_LENGTH = 10;
    public static final int REJECTION_REASON_MAX_LENGTH = 500;
    public static final int CHANGE_REASON_MAX_LENGTH = 500;

    // Transaction ID Validation
    public static final int TRANSACTION_ID_MIN_LENGTH = 5;
    public static final int TRANSACTION_ID_MAX_LENGTH = 100;

    // UPI ID Validation
    public static final String UPI_ID_PATTERN = "^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$";
    public static final int UPI_ID_MAX_LENGTH = 100;

    // Bank Account Validation
    public static final String BANK_ACCOUNT_PATTERN = "^[0-9]{9,18}$";
    public static final String IFSC_CODE_PATTERN = "^[A-Z]{4}0[A-Z0-9]{6}$";

    // GST Number Validation
    public static final String GST_NUMBER_PATTERN = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
    public static final int GST_NUMBER_LENGTH = 15;

    // Validation Messages
    public static final String MSG_INVALID_CHARGE_NAME = "Charge name must be 2-100 characters and contain only letters, numbers, spaces, and hyphens";
    public static final String MSG_INVALID_AMOUNT = "Amount must be between 0.01 and 999999.99";
    public static final String MSG_INVALID_RENT = "Rent must be between 100 and 1000000";
    public static final String MSG_INVALID_UPI_ID = "Invalid UPI ID format. Example: user@paytm";
    public static final String MSG_INVALID_BANK_ACCOUNT = "Bank account number must be 9-18 digits";
    public static final String MSG_INVALID_IFSC_CODE = "Invalid IFSC code format. Example: SBIN0001234";
    public static final String MSG_INVALID_GST_NUMBER = "Invalid GST number format. Must be 15 characters";
    public static final String MSG_INVALID_TRANSACTION_ID = "Transaction ID must be 5-100 characters";

    private BillingValidation() {
        // Private constructor to prevent instantiation
    }
}
