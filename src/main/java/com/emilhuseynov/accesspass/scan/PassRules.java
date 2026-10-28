package com.emilhuseynov.accesspass.scan;

import com.emilhuseynov.accesspass.pass.AccessPass;
import com.emilhuseynov.accesspass.token.PassClaims;
import java.time.Instant;
import java.util.Optional;

final class PassRules {

    private PassRules() {
    }

    static Optional<DenyReason> firstViolation(AccessPass pass, PassClaims claims, Long gateId, Instant now) {
        if (!pass.isIssuedFor(claims)) {
            return Optional.of(DenyReason.TOKEN_MISMATCH);
        }
        if (pass.isRevoked()) {
            return Optional.of(DenyReason.REVOKED);
        }
        if (!pass.getHolder().isActive()) {
            return Optional.of(DenyReason.HOLDER_INACTIVE);
        }
        if (now.isBefore(pass.getValidFrom())) {
            return Optional.of(DenyReason.NOT_YET_VALID);
        }
        if (!now.isBefore(pass.getValidUntil())) {
            return Optional.of(DenyReason.EXPIRED);
        }
        if (!pass.getPolicy().allowsGate(gateId)) {
            return Optional.of(DenyReason.GATE_NOT_ALLOWED);
        }
        if (!pass.getPolicy().allowsTimeOf(now)) {
            return Optional.of(DenyReason.OUTSIDE_TIME_WINDOW);
        }
        if (!pass.hasUsesLeft()) {
            return Optional.of(DenyReason.USES_EXHAUSTED);
        }
        return Optional.empty();
    }
}
