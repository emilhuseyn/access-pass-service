package com.emilhuseynov.accesspass.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.emilhuseynov.accesspass.token.TokenVerification.Failure;
import com.emilhuseynov.accesspass.token.TokenVerification.Invalid;
import com.emilhuseynov.accesspass.token.TokenVerification.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PassTokenCodecTest {

    private static final SecretKey KEY_2025 = key((byte) 1);
    private static final SecretKey KEY_2026 = key((byte) 2);
    private static final Instant EXPIRY = Instant.parse("2026-11-01T18:00:00Z");
    private static final Instant BEFORE_EXPIRY = EXPIRY.minusSeconds(3600);
    private static final PassClaims CLAIMS =
            new PassClaims(UUID.fromString("6f1c2d3e-4b5a-4c7d-8e9f-0a1b2c3d4e5f"), 42L, EXPIRY, "n0nce-value");

    private final PassTokenCodec codec = new PassTokenCodec(new SigningKeyRing("k2025", Map.of("k2025", KEY_2025)));

    @Test
    void signedTokenVerifiesAndCarriesClaimsAndKeyId() {
        String token = codec.sign(CLAIMS, "k2025");

        assertThat(token).startsWith("AP1.k2025.");
        assertThat(codec.verify(token, BEFORE_EXPIRY)).isEqualTo(new Valid(CLAIMS, "k2025"));
    }

    @Test
    void signingIsDeterministicSoTheQrCanBeRegenerated() {
        assertThat(codec.sign(CLAIMS, "k2025")).isEqualTo(codec.sign(CLAIMS, "k2025"));
    }

    @Test
    void rejectsPayloadSwappedForAnotherHolder() {
        String token = codec.sign(CLAIMS, "k2025");
        String forged = codec.sign(new PassClaims(CLAIMS.passId(), 43L, EXPIRY, CLAIMS.nonce()), "k2025");
        String tampered = replacePart(token, 2, part(forged, 2));

        assertThat(codec.verify(tampered, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.BAD_SIGNATURE));
    }

    @Test
    void rejectsPayloadWithExtendedExpiry() {
        String token = codec.sign(CLAIMS, "k2025");
        String payload = new String(Base64.getUrlDecoder().decode(part(token, 2)), StandardCharsets.UTF_8)
                .replace(String.valueOf(EXPIRY.getEpochSecond()), String.valueOf(EXPIRY.getEpochSecond() + 86_400));
        String tampered = replacePart(token, 2,
                Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));

        assertThat(codec.verify(tampered, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.BAD_SIGNATURE));
    }

    @Test
    void rejectsAlteredSignature() {
        String token = codec.sign(CLAIMS, "k2025");
        String signature = part(token, 3);
        char first = signature.charAt(0);
        String altered = (first == 'A' ? 'B' : 'A') + signature.substring(1);

        assertThat(codec.verify(replacePart(token, 3, altered), BEFORE_EXPIRY))
                .isEqualTo(new Invalid(Failure.BAD_SIGNATURE));
    }

    @Test
    void rejectsNonCanonicalSignatureEncoding() {
        String token = codec.sign(CLAIMS, "k2025");
        String signature = part(token, 3);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        char last = signature.charAt(signature.length() - 1);
        char sameBytesDifferentText = alphabet.charAt(alphabet.indexOf(last) ^ 1);
        String altered = signature.substring(0, signature.length() - 1) + sameBytesDifferentText;

        assertThat(codec.verify(replacePart(token, 3, altered), BEFORE_EXPIRY))
                .isEqualTo(new Invalid(Failure.MALFORMED));
    }

    @Test
    void rejectsUnknownKeyId() {
        String token = codec.sign(CLAIMS, "k2025");

        assertThat(codec.verify(replacePart(token, 1, "k1999"), BEFORE_EXPIRY))
                .isEqualTo(new Invalid(Failure.UNKNOWN_KEY));
    }

    @Test
    void rejectsTokenRelabelledWithAnotherValidKeyId() {
        PassTokenCodec twoKeys = new PassTokenCodec(
                new SigningKeyRing("k2026", Map.of("k2025", KEY_2025, "k2026", KEY_2026)));
        String token = twoKeys.sign(CLAIMS, "k2025");

        assertThat(twoKeys.verify(replacePart(token, 1, "k2026"), BEFORE_EXPIRY))
                .isEqualTo(new Invalid(Failure.BAD_SIGNATURE));
    }

    @Test
    void expiryIsExclusive() {
        String token = codec.sign(CLAIMS, "k2025");

        assertThat(codec.verify(token, EXPIRY.minusSeconds(1))).isInstanceOf(Valid.class);
        assertThat(codec.verify(token, EXPIRY)).isEqualTo(new Invalid(Failure.EXPIRED));
        assertThat(codec.verify(token, EXPIRY.plusSeconds(60))).isEqualTo(new Invalid(Failure.EXPIRED));
    }

    @Test
    void signatureIsCheckedBeforeExpirySoForgedTokensNeverLookMerelyExpired() {
        String token = codec.sign(CLAIMS, "k2025");
        String signature = part(token, 3);
        String altered = (signature.charAt(0) == 'A' ? 'B' : 'A') + signature.substring(1);

        assertThat(codec.verify(replacePart(token, 3, altered), EXPIRY.plusSeconds(60)))
                .isEqualTo(new Invalid(Failure.BAD_SIGNATURE));
    }

    @Nested
    class KeyRotation {

        private final PassTokenCodec beforeRotation =
                new PassTokenCodec(new SigningKeyRing("k2025", Map.of("k2025", KEY_2025)));
        private final PassTokenCodec duringRotation =
                new PassTokenCodec(new SigningKeyRing("k2026", Map.of("k2025", KEY_2025, "k2026", KEY_2026)));
        private final PassTokenCodec afterRetirement =
                new PassTokenCodec(new SigningKeyRing("k2026", Map.of("k2026", KEY_2026)));

        @Test
        void tokensSignedWithThePreviousKeyStillVerifyWhileItIsKept() {
            String oldToken = beforeRotation.sign(CLAIMS, beforeRotation.activeKeyId());

            assertThat(duringRotation.verify(oldToken, BEFORE_EXPIRY)).isEqualTo(new Valid(CLAIMS, "k2025"));
        }

        @Test
        void newTokensUseTheNewActiveKey() {
            String newToken = duringRotation.sign(CLAIMS, duringRotation.activeKeyId());

            assertThat(newToken).startsWith("AP1.k2026.");
            assertThat(duringRotation.verify(newToken, BEFORE_EXPIRY)).isEqualTo(new Valid(CLAIMS, "k2026"));
            assertThat(beforeRotation.verify(newToken, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.UNKNOWN_KEY));
        }

        @Test
        void tokensSignedWithARetiredKeyAreRejected() {
            String oldToken = beforeRotation.sign(CLAIMS, "k2025");

            assertThat(afterRetirement.verify(oldToken, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.UNKNOWN_KEY));
            assertThat(afterRetirement.hasKey("k2025")).isFalse();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "AP1",
            "AP1.k2025.payload",
            "AP2.k2025.cGF5bG9hZA.c2ln",
            "AP1.k2025.cGF5bG9hZA.c2ln.extra",
            "AP1.k2025.cGF5bG9hZA.not base64",
            "AP1.k2025.cGF5bG9hZA.c2ln=="
    })
    void rejectsMalformedTokens(String token) {
        assertThat(codec.verify(token, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.MALFORMED));
    }

    @Test
    void rejectsOversizedTokensWithoutComputingAnything() {
        String oversized = "AP1.k2025." + "A".repeat(600) + ".c2ln";

        assertThat(codec.verify(oversized, BEFORE_EXPIRY)).isEqualTo(new Invalid(Failure.MALFORMED));
    }

    @Test
    void keyRingRejectsShortKeysAndMissingActiveKey() {
        SecretKey shortKey = new SecretKeySpec(new byte[16], "HmacSHA256");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SigningKeyRing("weak", Map.of("weak", shortKey)))
                .withMessageContaining("at least 32 bytes");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SigningKeyRing("k2026", Map.of("k2025", KEY_2025)))
                .withMessageContaining("not configured");
    }

    private static SecretKey key(byte fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, fill);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static String part(String token, int index) {
        return token.split("\\.")[index];
    }

    private static String replacePart(String token, int index, String value) {
        String[] parts = token.split("\\.");
        parts[index] = value;
        return String.join(".", parts);
    }
}
