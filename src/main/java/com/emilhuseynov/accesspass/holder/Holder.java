package com.emilhuseynov.accesspass.holder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "holder")
public class Holder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(length = 254)
    private String email;

    @Column(name = "external_ref", length = 64)
    private String externalRef;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Holder() {
    }

    public Holder(String fullName, String email, String externalRef, Instant createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.externalRef = externalRef;
        this.active = true;
        this.createdAt = createdAt;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
