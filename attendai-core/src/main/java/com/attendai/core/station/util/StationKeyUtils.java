package com.attendai.core.station.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility methods for station API key generation and hashing.
 *
 * Extracted to a dedicated class so both {@link com.attendai.core.station.service.StationServiceImpl}
 * and {@link com.attendai.core.station.filter.StationAuthenticationFilter} can share
 * the same hashing logic without a cross-package dependency on an internal service method.
 */
public final class StationKeyUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private StationKeyUtils() {}

    /**
     * Generates a cryptographically random 32-byte API key,
     * Base64 URL-safe encoded, prefixed with "ak_".
     */
    public static String generateRawApiKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Returns the SHA-256 hex digest of the given input string.
     * Used for all API key storage and lookup operations.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
