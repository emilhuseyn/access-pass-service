package com.emilhuseynov.accesspass.pass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class PassDtos {

    private PassDtos() {
    }

    public record IssuePassRequest(
            @NotNull Long holderId,
            @NotNull Long policyId,
            Instant validFrom,
            @NotNull Instant validUntil) {
    }

    public record RevokePassRequest(@NotBlank @Size(max = 200) String reason) {
    }

    public record IssuedPassResponse(
            UUID passId,
            String token,
            String qrCodeUrl,
            Instant validFrom,
            Instant validUntil,
            Integer maxUses) {

        static IssuedPassResponse from(IssuedPass issued) {
            AccessPass pass = issued.pass();
            return new IssuedPassResponse(pass.getId(), issued.token(), "/api/v1/passes/" + pass.getId() + "/qr",
                    pass.getValidFrom(), pass.getValidUntil(), pass.getMaxUses());
        }
    }

    public record PassResponse(
            UUID id,
            Long holderId,
            Long policyId,
            PassStatus status,
            Instant validFrom,
            Instant validUntil,
            Integer maxUses,
            int useCount,
            String keyId,
            Instant issuedAt,
            String issuedBy,
            Instant revokedAt,
            String revokedBy,
            String revocationReason) {

        static PassResponse from(AccessPass pass) {
            return new PassResponse(
                    pass.getId(),
                    pass.getHolder().getId(),
                    pass.getPolicy().getId(),
                    pass.getStatus(),
                    pass.getValidFrom(),
                    pass.getValidUntil(),
                    pass.getMaxUses(),
                    pass.getUseCount(),
                    pass.getKeyId(),
                    pass.getIssuedAt(),
                    pass.getIssuedBy(),
                    pass.getRevokedAt(),
                    pass.getRevokedBy(),
                    pass.getRevocationReason());
        }
    }
}
