package com.trustdoc.docs;

import com.trustdoc.common.AccessLevel;
import com.trustdoc.common.Integrity;
import java.time.Instant;

public record DocumentDto(Long id, String name, AccessLevel accessLevel, int pages, int chunks, String sha256,
                          Instant uploadedAt, String uploadedBy, Integrity integrity, Instant lastCheckedAt) {

    static DocumentDto of(DocumentEntity d) {
        return new DocumentDto(d.getId(), d.getName(), d.getAccessLevel(), d.getPages(), d.getChunks(), d.getSha256(),
            d.getUploadedAt(), d.getUploadedBy(), d.getIntegrity(), d.getLastCheckedAt());
    }
}
