package com.trustdoc.docs;

import com.trustdoc.common.AccessLevel;
import com.trustdoc.common.Integrity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Nom du fichier stocké sur le disque (dans trustdoc.storage.dir). */
    private String storedFile;

    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    private int pages;
    private int chunks;

    /** Empreinte SHA-256 calculée à l'import : la référence pour vérifier l'intégrité. */
    @Column(length = 64)
    private String sha256;

    private Instant uploadedAt;
    private String uploadedBy;

    @Enumerated(EnumType.STRING)
    private Integrity integrity = Integrity.NON_VERIFIE;

    private Instant lastCheckedAt;

    protected DocumentEntity() {}

    public DocumentEntity(String name, String storedFile, AccessLevel accessLevel, String sha256, String uploadedBy) {
        this.name = name;
        this.storedFile = storedFile;
        this.accessLevel = accessLevel;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Instant.now();
        this.integrity = Integrity.OK;
        this.lastCheckedAt = this.uploadedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStoredFile() { return storedFile; }
    public AccessLevel getAccessLevel() { return accessLevel; }
    public int getPages() { return pages; }
    public int getChunks() { return chunks; }
    public String getSha256() { return sha256; }
    public Instant getUploadedAt() { return uploadedAt; }
    public String getUploadedBy() { return uploadedBy; }
    public Integrity getIntegrity() { return integrity; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }

    public void setPages(int pages) { this.pages = pages; }
    public void setChunks(int chunks) { this.chunks = chunks; }
    public void setStoredFile(String storedFile) { this.storedFile = storedFile; }

    public void markChecked(Integrity result) {
        this.integrity = result;
        this.lastCheckedAt = Instant.now();
    }
}
