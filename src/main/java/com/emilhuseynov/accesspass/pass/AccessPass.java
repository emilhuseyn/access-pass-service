package com.emilhuseynov.accesspass.pass;

import com.emilhuseynov.accesspass.holder.Holder;
import com.emilhuseynov.accesspass.policy.PassPolicy;
import com.emilhuseynov.accesspass.token.PassClaims;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "access_pass")
public class AccessPass {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holder_id", nullable = false, updatable = false)
    private Holder holder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, updatable = false)
    private PassPolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PassStatus status;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false, updatable = false)
    private Instant validUntil;

    @Column(name = "max_uses", updatable = false)
    private Integer maxUses;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Column(name = "key_id", nullable = false, length = 32, updatable = false)
    private String keyId;

    @Column(nullable = false, length = 32, updatable = false)
    private String nonce;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "issued_by", nullable = false, length = 64, updatable = false)
    private String issuedBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 64)
    private String revokedBy;

    @Column(name = "revocation_reason", length = 200)
    private String revocationReason;

    @Version
    private Long version;

    protected AccessPass() {
    }

    AccessPass(UUID id, Holder holder, PassPolicy policy, Instant validFrom, Instant validUntil, String keyId,
               String nonce, Instant issuedAt, String issuedBy) {
        this.id = id;
        this.holder = holder;
        this.policy = policy;
        this.status = PassStatus.ACTIVE;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.maxUses = policy.getMaxUses();
        this.keyId = keyId;
        this.nonce = nonce;
        this.issuedAt = issuedAt;
        this.issuedBy = issuedBy;
    }

    public PassClaims claims() {
        return new PassClaims(id, holder.getId(), validUntil, nonce);
    }

    public boolean isIssuedFor(PassClaims claims) {
        return claims().equals(claims);
    }

    void revoke(Instant at, String by, String reason) {
        this.status = PassStatus.REVOKED;
        this.revokedAt = at;
        this.revokedBy = by;
        this.revocationReason = reason;
    }

    public boolean isRevoked() {
        return status == PassStatus.REVOKED;
    }

    public boolean hasUsesLeft() {
        return maxUses == null || useCount < maxUses;
    }

    public UUID getId() {
        return id;
    }

    public Holder getHolder() {
        return holder;
    }

    public PassPolicy getPolicy() {
        return policy;
    }

    public PassStatus getStatus() {
        return status;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public int getUseCount() {
        return useCount;
    }

    public String getKeyId() {
        return keyId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }
}
