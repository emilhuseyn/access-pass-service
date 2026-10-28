package com.emilhuseynov.accesspass.scan;

import com.emilhuseynov.accesspass.token.TokenVerification;

public enum DenyReason {
    MALFORMED_TOKEN,
    UNKNOWN_SIGNING_KEY,
    INVALID_SIGNATURE,
    EXPIRED,
    UNKNOWN_PASS,
    TOKEN_MISMATCH,
    REVOKED,
    HOLDER_INACTIVE,
    NOT_YET_VALID,
    GATE_NOT_ALLOWED,
    OUTSIDE_TIME_WINDOW,
    USES_EXHAUSTED,
    CONCURRENT_SCAN;

    static DenyReason of(TokenVerification.Failure failure) {
        return switch (failure) {
            case MALFORMED -> MALFORMED_TOKEN;
            case UNKNOWN_KEY -> UNKNOWN_SIGNING_KEY;
            case BAD_SIGNATURE -> INVALID_SIGNATURE;
            case EXPIRED -> EXPIRED;
        };
    }
}
