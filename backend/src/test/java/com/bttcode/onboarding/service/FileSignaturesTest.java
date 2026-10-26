package com.bttcode.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileSignaturesTest {

    @Test
    void detectsSupportedTypesFromTheirMagicBytes() {
        assertThat(FileSignatures.detectMediaType("%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII)))
                .contains("application/pdf");
        assertThat(FileSignatures.detectMediaType(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0}))
                .contains("image/png");
        assertThat(FileSignatures.detectMediaType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}))
                .contains("image/jpeg");
    }

    @Test
    void rejectsAnExecutableRenamedToPdf() {
        byte[] windowsExecutable = {'M', 'Z', (byte) 0x90, 0};

        assertThat(FileSignatures.detectMediaType(windowsExecutable)).isEmpty();
    }

    @Test
    void rejectsContentShorterThanASignature() {
        assertThat(FileSignatures.detectMediaType(new byte[] {'%'})).isEmpty();
    }

    @Test
    void sanitizesFileNames() {
        assertThat(FileSignatures.sanitizeFileName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileSignatures.sanitizeFileName("C:\\Users\\anna\\ID card (1).pdf")).isEqualTo("ID_card__1_.pdf");
        assertThat(FileSignatures.sanitizeFileName("..")).isEqualTo("document");
        assertThat(FileSignatures.sanitizeFileName(null)).isEqualTo("document");
        assertThat(FileSignatures.sanitizeFileName("a".repeat(300) + ".pdf")).hasSize(100).endsWith(".pdf");
    }
}
