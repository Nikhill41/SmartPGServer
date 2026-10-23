package com.pgmanagement.entity;

import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.enums.RoomType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing individual rooms within a PG property
 */
@Entity
@Table(name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_room_per_pg", columnNames = {"pg_id", "room_number"})
        },
        indexes = {
                @Index(name = "idx_rooms_pg", columnList = "pg_id"),
                @Index(name = "idx_rooms_status", columnList = "status"),
                @Index(name = "idx_rooms_type", columnList = "type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false)
    private Integer capacity; // Max number of tenants

    @Column(name = "current_occupancy", nullable = false)
    private Integer currentOccupancy = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    /**
     * Array of S3 image URLs (max 10 images enforced in service layer)
     */
    @ElementCollection
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private List<Amenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<User> tenants = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<BookingRequest> bookingRequests = new ArrayList<>();

    /**
     * Validation: Max 10 images allowed
     */
    public void addImageUrl(String imageUrl) {
        if (this.imageUrls.size() >= 10) {
            throw new IllegalStateException("Maximum 10 images allowed per room");
        }
        this.imageUrls.add(imageUrl);
    }

    /**
     * Business logic: Check if room has available space
     */
    public boolean hasAvailableSpace() {
        return this.currentOccupancy < this.capacity;
    }

    /**
     * Business logic: Increment occupancy and update status
     */
    public void incrementOccupancy() {
        if (!hasAvailableSpace()) {
            throw new IllegalStateException("Room is already full");
        }
        this.currentOccupancy++;
        if (this.currentOccupancy.equals(this.capacity)) {
            this.status = RoomStatus.OCCUPIED;
        }
    }

    /**
     * Business logic: Decrement occupancy when tenant leaves
     */
    public void decrementOccupancy() {
        if (this.currentOccupancy > 0) {
            this.currentOccupancy--;
            this.status = RoomStatus.AVAILABLE;
        }
    }
}
