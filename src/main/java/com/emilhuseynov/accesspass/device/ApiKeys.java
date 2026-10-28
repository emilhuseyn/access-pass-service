package com.emilhuseynov.accesspass.device;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class ApiKeys {

    private static final String PREFIX = "gdk_";
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeys() {
    }

    static String generate() {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
