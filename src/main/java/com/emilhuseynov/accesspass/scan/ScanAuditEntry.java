package com.emilhuseynov.accesspass.scan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "scan_audit")
public class ScanAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "gate_id", nullable = false)
    private Long gateId;

    @Column(name = "pass_id")
    private UUID passId;

    @Column(name = "holder_id")
    private Long holderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScanOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DenyReason reason;

    @Column(name = "token_fingerprint", nullable = false, length = 16)
    private String tokenFingerprint;

    protected ScanAuditEntry() {
    }

    ScanAuditEntry(Instant occurredAt, Long deviceId, Long gateId, ScanDecision decision, String tokenFingerprint) {
        this.occurredAt = occurredAt;
        this.deviceId = deviceId;
        this.gateId = gateId;
        this.passId = decision.passId();
        this.holderId = decision.holderId();
        this.outcome = decision.outcome();
        this.reason = decision.reason();
        this.tokenFingerprint = tokenFingerprint;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getGateId() {
        return gateId;
    }

    public UUID getPassId() {
        return passId;
    }

    public Long getHolderId() {
        return holderId;
    }

    public ScanOutcome getOutcome() {
        return outcome;
    }

    public DenyReason getReason() {
        return reason;
    }

    public String getTokenFingerprint() {
        return tokenFingerprint;
    }
}
