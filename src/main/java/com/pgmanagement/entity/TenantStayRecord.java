    package com.pgmanagement.entity;



    import com.pgmanagement.enums.StayStatus;
    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDate;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "tenant_stay_records")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class TenantStayRecord {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "tenant_id", nullable = false)
        private Long tenantId;

        @Column(name = "room_id", nullable = false)
        private Long roomId;

        @Column(name = "booking_id")
        private Long bookingId;

        @Column(name = "move_in_date", nullable = false)
        private LocalDate moveInDate;

        @Column(name = "move_out_date")
        private LocalDate moveOutDate;

        @Enumerated(EnumType.STRING)
        @Column(name = "stay_status", nullable = false, length = 20)
        private StayStatus stayStatus = StayStatus.ACTIVE;

        @Column(name = "days_stayed")
        private Integer daysStayed;

        @Column(name = "checkout_initiated")
        private Boolean checkoutInitiated = false;

        @Column(name = "intended_move_out_date")
        private LocalDate intendedMoveOutDate;

        @Column(name = "checkout_completed_at")
        private LocalDateTime checkoutCompletedAt;

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
        @JoinColumn(name = "booking_id", referencedColumnName = "id", insertable = false, updatable = false)
        private BookingRequest booking;
    }

