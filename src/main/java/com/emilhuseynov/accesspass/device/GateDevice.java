package com.emilhuseynov.accesspass.device;

import com.emilhuseynov.accesspass.site.Gate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "gate_device")
public class GateDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_id", nullable = false, updatable = false)
    private Gate gate;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    protected GateDevice() {
    }

    GateDevice(Gate gate, String name, String apiKeyHash, Instant createdAt) {
        this.gate = gate;
        this.name = name;
        this.apiKeyHash = apiKeyHash;
        this.active = true;
        this.createdAt = createdAt;
    }

    void replaceApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    void deactivate(Instant at) {
        this.active = false;
        this.deactivatedAt = at;
    }

    public Long getId() {
        return id;
    }

    public Gate getGate() {
        return gate;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
