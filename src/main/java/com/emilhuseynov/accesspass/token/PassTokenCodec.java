package com.emilhuseynov.accesspass.token;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.emilhuseynov.accesspass.token.TokenVerification.Failure;
import com.emilhuseynov.accesspass.token.TokenVerification.Invalid;
import com.emilhuseynov.accesspass.token.TokenVerification.Valid;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/**
 * Compact pass token: {@code AP1.<keyId>.<payload>.<signature>}, where payload is the base64url encoding of
 * {@code passId|holderId|expiryEpochSeconds|nonce} and signature is HMAC-SHA256 over everything before the last dot.
 */
public final class PassTokenCodec {

    static final String VERSION = "AP1";
    private static final int MAX_TOKEN_LENGTH = 512;
    private static final Pattern TOKEN_ALPHABET = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SigningKeyRing keyRing;

    public PassTokenCodec(SigningKeyRing keyRing) {
        this.keyRing = keyRing;
    }

    public String activeKeyId() {
        return keyRing.activeKeyId();
    }

    public boolean hasKey(String keyId) {
        return keyRing.find(keyId).isPresent();
    }

    public String sign(PassClaims claims, String keyId) {
        SecretKey key = keyRing.find(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Signing key '" + keyId + "' is not configured"));
        String payload = ENCODER.encodeToString(serialize(claims).getBytes(UTF_8));
        String signingInput = VERSION + "." + keyId + "." + payload;
        return signingInput + "." + ENCODER.encodeToString(hmac(key, signingInput));
    }

    public TokenVerification verify(String token, Instant now) {
        if (token == null || token.length() > MAX_TOKEN_LENGTH || !TOKEN_ALPHABET.matcher(token).matches()) {
            return new Invalid(Failure.MALFORMED);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            return new Invalid(Failure.MALFORMED);
        }
        String keyId = parts[1];
        Optional<SecretKey> key = keyRing.find(keyId);
        if (key.isEmpty()) {
            return new Invalid(Failure.UNKNOWN_KEY);
        }
        Optional<byte[]> signature = decodeCanonical(parts[3]);
        if (signature.isEmpty()) {
            return new Invalid(Failure.MALFORMED);
        }
        byte[] expected = hmac(key.get(), parts[0] + "." + keyId + "." + parts[2]);
        if (!MessageDigest.isEqual(expected, signature.get())) {
            return new Invalid(Failure.BAD_SIGNATURE);
        }
        Optional<PassClaims> claims = decodeCanonical(parts[2]).flatMap(PassTokenCodec::deserialize);
        if (claims.isEmpty()) {
            return new Invalid(Failure.MALFORMED);
        }
        if (!now.isBefore(claims.get().expiresAt())) {
            return new Invalid(Failure.EXPIRED);
        }
        return new Valid(claims.get(), keyId);
    }

    private static String serialize(PassClaims claims) {
        return claims.passId() + "|" + claims.holderId() + "|" + claims.expiresAt().getEpochSecond() + "|"
                + claims.nonce();
    }

    private static Optional<PassClaims> deserialize(byte[] payload) {
        String[] fields = new String(payload, UTF_8).split("\\|", -1);
        if (fields.length != 4) {
            return Optional.empty();
        }
        try {
            return Optional.of(new PassClaims(
                    UUID.fromString(fields[0]),
                    Long.parseLong(fields[1]),
                    Instant.ofEpochSecond(Long.parseLong(fields[2])),
                    fields[3]));
        } catch (IllegalArgumentException | DateTimeException ex) {
            return Optional.empty();
        }
    }

    private static Optional<byte[]> decodeCanonical(String encoded) {
        try {
            byte[] decoded = DECODER.decode(encoded);
            return ENCODER.encodeToString(decoded).equals(encoded) ? Optional.of(decoded) : Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static byte[] hmac(SecretKey key, String input) {
        try {
            Mac mac = Mac.getInstance(SigningKeyRing.ALGORITHM);
            mac.init(key);
            return mac.doFinal(input.getBytes(UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }
}
