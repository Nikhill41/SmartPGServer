package com.pgmanagement.constants;


public class BillingSuccessMessages {

    // Configuration
    public static final String CONFIG_CREATED = "Billing configuration created successfully";
    public static final String CONFIG_UPDATED = "Billing configuration updated successfully";

    // Standard Charges
    public static final String CHARGE_CREATED = "Standard charge created successfully";
    public static final String CHARGE_UPDATED = "Standard charge updated successfully";
    public static final String CHARGE_DELETED = "Standard charge deleted successfully";
    public static final String CHARGE_ACTIVATED = "Standard charge activated";
    public static final String CHARGE_DEACTIVATED = "Standard charge deactivated";

    // Bills
    public static final String BILL_GENERATED = "Bill generated successfully";
    public static final String BILL_UPDATED = "Bill updated successfully";
    public static final String BILL_CANCELLED = "Bill cancelled successfully";
    public static final String BILLS_GENERATED_BULK = "%d bills generated successfully";

    // Payment
    public static final String PAYMENT_PROOF_UPLOADED = "Payment proof uploaded successfully. Awaiting owner verification.";
    public static final String PAYMENT_VERIFIED = "Payment verified successfully";
    public static final String PAYMENT_REJECTED = "Payment proof rejected. Tenant can re-upload.";
    public static final String LATE_FEE_WAIVED = "Late fee waived successfully";

    // Checkout
    public static final String CHECKOUT_INITIATED = "Checkout request submitted successfully";
    public static final String CHECKOUT_COMPLETED = "Checkout completed successfully";
    public static final String FINAL_BILL_GENERATED = "Final settlement bill generated";

    // Reminders
    public static final String REMINDER_SENT = "Payment reminder sent successfully";

    private BillingSuccessMessages() {
        // Private constructor to prevent instantiation
    }
}

