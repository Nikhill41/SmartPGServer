package com.pgmanagement.entity;

import com.pgmanagement.enums.GenderPreference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing PG (Paying Guest) properties
 */
@Entity
@Table(name = "pgs", indexes = {
        @Index(name = "idx_pgs_owner", columnList = "owner_id"),
        @Index(name = "idx_pgs_city", columnList = "city"),
        @Index(name = "idx_pgs_gender", columnList = "gender_preference")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PG {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", length = 10)
    private GenderPreference genderPreference;

    /**
     * Array of S3 image URLs (max 5 images enforced in service layer)
     */
    @ElementCollection
    @CollectionTable(name = "pg_images", joinColumns = @JoinColumn(name = "pg_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "pg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    /**
     * Validation: Max 5 images allowed
     */
    public void addImageUrl(String imageUrl) {
        if (this.imageUrls.size() >= 5) {
            throw new IllegalStateException("Maximum 5 images allowed per PG");
        }
        this.imageUrls.add(imageUrl);
    }
}
