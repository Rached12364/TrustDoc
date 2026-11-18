package com.trustdoc.docs;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChunkRepository extends JpaRepository<ChunkEntity, Long> {

    List<ChunkEntity> findByDocumentIdOrderByPositionAsc(Long documentId);

    @Transactional
    void deleteByDocumentId(Long documentId);
}
