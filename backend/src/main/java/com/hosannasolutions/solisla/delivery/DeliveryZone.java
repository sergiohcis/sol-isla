package com.hosannasolutions.solisla.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Design doc §24: keep delivery fee configuration separate from order pricing logic — deliberately
 *  simple for MVP (flat fee per zone), not postal-code rules or distance calculation yet. */
@Entity
@Table(name = "delivery_zones")
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal fee;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeliveryZone() {
    }

    public DeliveryZone(String name, BigDecimal fee) {
        Instant now = Instant.now();
        this.name = name;
        this.fee = fee;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, BigDecimal fee, boolean active) {
        this.name = name;
        this.fee = fee;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
