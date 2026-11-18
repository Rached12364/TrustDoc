package com.trustdoc.docs;

import com.trustdoc.common.AccessLevel;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    List<DocumentEntity> findByAccessLevelInOrderByIdAsc(Collection<AccessLevel> levels);

    boolean existsBySha256(String sha256);
}
