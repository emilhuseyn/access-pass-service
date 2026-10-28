package com.emilhuseynov.accesspass.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Site() {
    }

    public Site(String name, ZoneId zone, Instant createdAt) {
        this.name = name;
        this.timeZone = zone.getId();
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ZoneId getZone() {
        return ZoneId.of(timeZone);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
