package com.pgmanagement.entity;



import com.pgmanagement.enums.BillingMode;
import com.pgmanagement.enums.ElectricitySplitMethod;
import com.pgmanagement.enums.ProRatedRentMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pg_billing_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PGBillingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_id", nullable = false, unique = true)
    private Long pgId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Payment Accounts (PG-specific)
    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "upi_name", length = 100)
    private String upiName;

    @Column(name = "bank_account_number", length = 20)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 11)
    private String bankIfscCode;

    @Column(name = "bank_account_name", length = 100)
    private String bankAccountName;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    // GST Configuration
    @Column(name = "gst_enabled", nullable = false)
    private Boolean gstEnabled = false;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    // Billing Mode
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false, length = 20)
    private BillingMode billingMode = BillingMode.MANUAL;

    @Column(name = "auto_generation_day", nullable = false)
    private Integer autoGenerationDay = 1;

    @Column(name = "due_days_offset", nullable = false)
    private Integer dueDaysOffset = 7;

    // Electricity
    @Column(name = "electricity_rate_per_unit", precision = 10, scale = 2)
    private BigDecimal electricityRatePerUnit = new BigDecimal("8.5");

    @Column(name = "include_electricity", nullable = false)
    private Boolean includeElectricity = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "electricity_split_method", length = 30)
    private ElectricitySplitMethod electricitySplitMethod = ElectricitySplitMethod.TIME_WEIGHTED;

    // Late Fees
    @Column(name = "late_fee_enabled", nullable = false)
    private Boolean lateFeeEnabled = false;

    @Column(name = "late_fee_amount", precision = 10, scale = 2)
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "late_fee_days")
    private Integer lateFeeDays = 3;

    // Pro-rated Rent
    @Enumerated(EnumType.STRING)
    @Column(name = "pro_rated_rent_mode", length = 20)
    private ProRatedRentMode proRatedRentMode = ProRatedRentMode.DAILY;

    // Security Deposit
    @Column(name = "security_deposit_amount", precision = 10, scale = 2)
    private BigDecimal securityDepositAmount = new BigDecimal("10000");

    @Column(name = "security_deposit_due_days")
    private Integer securityDepositDueDays = 7;

    // Standard Charges
    @Column(name = "auto_apply_standard_charges", nullable = false)
    private Boolean autoApplyStandardCharges = true;

    @Column(name = "pro_rate_standard_charges", nullable = false)
    private Boolean proRateStandardCharges = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PG pg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Owner owner;
}
