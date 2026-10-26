package com.bttcode.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * The supporting document, kept in its own table so that listing applications never loads file
 * contents. One document per application; uploading again replaces it.
 */
@Entity
@Table(name = "application_document")
public class ApplicationDocument {

    @Id
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ApplicationDocument() {
        // for JPA
    }

    public ApplicationDocument(UUID applicationId, String fileName, String contentType, byte[] data, Instant uploadedAt) {
        this.applicationId = applicationId;
        replace(fileName, contentType, data, uploadedAt);
    }

    public void replace(String fileName, String contentType, byte[] data, Instant uploadedAt) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.uploadedAt = uploadedAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
