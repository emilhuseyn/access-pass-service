package com.emilhuseynov.accesspass.token;

public sealed interface TokenVerification {

    record Valid(PassClaims claims, String keyId) implements TokenVerification {
    }

    record Invalid(Failure failure) implements TokenVerification {
    }

    enum Failure {
        MALFORMED,
        UNKNOWN_KEY,
        BAD_SIGNATURE,
        EXPIRED
    }
}
