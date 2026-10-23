package com.pgmanagement.entity;

import com.pgmanagement.enums.MeterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing IoT smart meters installed in rooms
 * Each meter monitors electricity consumption and can be controlled remotely
 */
@Entity
@Table(name = "meters",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_meter_id", columnNames = "meter_id"),
                @UniqueConstraint(name = "unique_aws_thing_name", columnNames = "aws_thing_name"),
                @UniqueConstraint(name = "unique_room_meter", columnNames = "room_id")
        },
        indexes = {
                @Index(name = "idx_meter_id", columnList = "meter_id"),
                @Index(name = "idx_meter_room", columnList = "room_id"),
                @Index(name = "idx_meter_owner", columnList = "owner_id"),
                @Index(name = "idx_meter_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique meter identifier (e.g., "METER_001")
     * This is also used as the AWS IoT Thing Name
     */
    @Column(name = "meter_id", nullable = false, unique = true, length = 50)
    private String meterId;

    /**
     * AWS IoT Thing Name (always equal to meterId)
     * Used for AWS IoT Core operations
     */
    @Column(name = "aws_thing_name", nullable = false, unique = true, length = 50)
    private String awsThingName;

    /**
     * Room to which this meter is assigned
     * One room can have at most one meter
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    /**
     * Owner of the PG where this meter is installed
     * Derived from room → PG → owner, but stored for quick access
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * Current status of the meter
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeterStatus status = MeterStatus.UNPAIRED;

    /**
     * Timestamp when meter was paired to the room
     */
    @Column(name = "paired_at")
    private LocalDateTime pairedAt;

    /**
     * Timestamp when meter was unpaired from the room
     */
    @Column(name = "unpaired_at")
    private LocalDateTime unpairedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if meter is currently operational
     */
    @Transient
    public boolean isOperational() {
        return this.status == MeterStatus.ACTIVE;
    }

    /**
     * Check if meter is paired to a room
     */
    @Transient
    public boolean isPaired() {
        return this.status != MeterStatus.UNPAIRED;
    }
}
