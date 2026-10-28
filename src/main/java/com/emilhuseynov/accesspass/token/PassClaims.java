package com.emilhuseynov.accesspass.token;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PassClaims(UUID passId, long holderId, Instant expiresAt, String nonce) {

    public PassClaims {
        Objects.requireNonNull(passId, "passId");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(nonce, "nonce");
        if (nonce.isEmpty() || nonce.contains("|")) {
            throw new IllegalArgumentException("Nonce must be non-empty and must not contain '|'");
        }
        if (expiresAt.getNano() != 0) {
            throw new IllegalArgumentException("Expiry must be whole seconds");
        }
    }
}
