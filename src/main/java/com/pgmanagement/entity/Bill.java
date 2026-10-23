package com.pgmanagement.entity;



import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.enums.GenerationType;
import com.pgmanagement.enums.PaymentProofStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    private String billNumber;

    // References
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "pg_id", nullable = false)
    private Long pgId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Billing Period
    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    // Charges Breakdown
    @Column(name = "room_rent", precision = 10, scale = 2)
    private BigDecimal roomRent = BigDecimal.ZERO;

    @Column(name = "electricity_charge", precision = 10, scale = 2)
    private BigDecimal electricityCharge = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 10, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "additional_deductions", precision = 10, scale = 2)
    private BigDecimal additionalDeductions = BigDecimal.ZERO;

    // GST
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "gst_enabled")
    private Boolean gstEnabled = false;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    // Security Deposit Adjustment
    @Column(name = "security_deposit_used", precision = 10, scale = 2)
    private BigDecimal securityDepositUsed = BigDecimal.ZERO;

    // Total
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Electricity Details
    @Column(name = "total_room_units", precision = 10, scale = 2)
    private BigDecimal totalRoomUnits;

    @Column(name = "tenant_unit_share", precision = 10, scale = 2)
    private BigDecimal tenantUnitShare;

    @Column(name = "total_room_tenants")
    private Integer totalRoomTenants;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillStatus status = BillStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", nullable = false, length = 30)
    private BillType billType = BillType.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 20)
    private GenerationType generationType = GenerationType.MANUAL;

    // Dates
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    // Payment Proof
    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Column(name = "payment_proof_uploaded_at")
    private LocalDateTime paymentProofUploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_proof_status", length = 20)
    private PaymentProofStatus paymentProofStatus;

    @Column(name = "payment_rejection_count")
    private Integer paymentRejectionCount = 0;

    @Column(name = "last_rejection_reason", columnDefinition = "TEXT")
    private String lastRejectionReason;

    @Column(name = "owner_verification_notes", columnDefinition = "TEXT")
    private String ownerVerificationNotes;

    @Column(name = "owner_verified_at")
    private LocalDateTime ownerVerifiedAt;

    // Versioning
    @Column(name = "version")
    private Integer version = 1;

    // Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

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
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", referencedColumnName = "id", insertable = false, updatable = false)
    private PG pg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Owner owner;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillCharge> billCharges = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    private List<BillVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    private List<PaymentProofHistory> paymentProofHistory = new ArrayList<>();
}

