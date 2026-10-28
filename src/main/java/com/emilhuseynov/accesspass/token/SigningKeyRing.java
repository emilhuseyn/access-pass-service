package com.emilhuseynov.accesspass.token;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class SigningKeyRing {

    static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_KEY_BYTES = 32;
    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final String activeKeyId;
    private final Map<String, SecretKey> keys;

    public SigningKeyRing(String activeKeyId, Map<String, SecretKey> keys) {
        keys.forEach(SigningKeyRing::validate);
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("Active signing key '" + activeKeyId + "' is not configured");
        }
        this.activeKeyId = activeKeyId;
        this.keys = Map.copyOf(keys);
    }

    public static SigningKeyRing fromProperties(TokenProperties properties) {
        Map<String, SecretKey> keys = properties.keys().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> decode(entry.getKey(), entry.getValue())));
        return new SigningKeyRing(properties.activeKeyId(), keys);
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public Optional<SecretKey> find(String keyId) {
        return Optional.ofNullable(keys.get(keyId));
    }

    private static SecretKey decode(String keyId, String base64Secret) {
        try {
            return new SecretKeySpec(Base64.getDecoder().decode(base64Secret.strip()), ALGORITHM);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Signing key '" + keyId + "' is not valid base64", ex);
        }
    }

    private static void validate(String keyId, SecretKey key) {
        if (!KEY_ID.matcher(keyId).matches()) {
            throw new IllegalArgumentException("Signing key id '" + keyId + "' must match " + KEY_ID.pattern());
        }
        if (key.getEncoded().length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "Signing key '" + keyId + "' must be at least " + MIN_KEY_BYTES + " bytes");
        }
    }
}
