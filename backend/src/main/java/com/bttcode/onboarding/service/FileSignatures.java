package com.bttcode.onboarding.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Detects the real file type from its first bytes. The Content-Type sent by the browser is chosen
 * by the client and proves nothing, so the stored media type always comes from here.
 */
public final class FileSignatures {

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private FileSignatures() {
    }

    public static Optional<String> detectMediaType(byte[] content) {
        if (startsWith(content, PDF)) {
            return Optional.of("application/pdf");
        }
        if (startsWith(content, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(content, JPEG)) {
            return Optional.of("image/jpeg");
        }
        return Optional.empty();
    }

    /** Keeps the base name only and replaces anything outside a safe character set. */
    public static String sanitizeFileName(String original) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.chars().allMatch(c -> c == '.' || c == '_')) {
            name = "document";
        }
        return name.length() <= 100 ? name : name.substring(name.length() - 100);
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
