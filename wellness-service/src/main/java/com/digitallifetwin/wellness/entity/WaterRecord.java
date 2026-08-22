package com.digitallifetwin.wellness.entity;

import com.digitallifetwin.wellness.enums.BeverageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "water_records")
public class WaterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "quantity_ml", nullable = false)
    private Integer quantityMl;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "beverage_type", nullable = false, length = 20)
    private BeverageType beverageType;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        deleted = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void softDelete() {
        deleted = true;
        deletedAt = Instant.now();
    }
}
