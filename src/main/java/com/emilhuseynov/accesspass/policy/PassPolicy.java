package com.emilhuseynov.accesspass.policy;

import com.emilhuseynov.accesspass.site.Gate;
import com.emilhuseynov.accesspass.site.Site;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pass_policy")
public class PassPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false, updatable = false)
    private Site site;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "pass_policy_gate",
            joinColumns = @JoinColumn(name = "policy_id"),
            inverseJoinColumns = @JoinColumn(name = "gate_id"))
    private Set<Gate> gates = new HashSet<>();

    @Column(name = "window_start")
    private LocalTime windowStart;

    @Column(name = "window_end")
    private LocalTime windowEnd;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PassPolicy() {
    }

    public PassPolicy(Site site, String name, Collection<Gate> gates, DailyWindow window, Integer maxUses,
                      Instant createdAt) {
        this.site = site;
        this.name = name;
        this.gates = new HashSet<>(gates);
        if (window != null) {
            this.windowStart = window.start();
            this.windowEnd = window.end();
        }
        this.maxUses = maxUses;
        this.createdAt = createdAt;
    }

    public boolean allowsGate(Long gateId) {
        return gates.stream().anyMatch(gate -> gate.getId().equals(gateId));
    }

    public boolean allowsTimeOf(Instant instant) {
        DailyWindow window = getWindow();
        return window == null || window.contains(instant.atZone(site.getZone()).toLocalTime());
    }

    public DailyWindow getWindow() {
        return windowStart == null ? null : new DailyWindow(windowStart, windowEnd);
    }

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public String getName() {
        return name;
    }

    public Set<Gate> getGates() {
        return Set.copyOf(gates);
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
