package com.trustdoc.docs;

import com.trustdoc.audit.AuditService;
import com.trustdoc.auth.AppUser;
import com.trustdoc.common.AccessLevel;
import com.trustdoc.common.Integrity;
import com.trustdoc.search.Bm25Index;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Import, intégrité et suppression des documents ; garde l'index de recherche à jour. */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final FileStorage storage;
    private final PdfExtractor extractor;
    private final Chunker chunker;
    private final Bm25Index index;
    private final AuditService audit;

    public DocumentService(DocumentRepository documents, ChunkRepository chunks, FileStorage storage,
                           PdfExtractor extractor, Chunker chunker, Bm25Index index, AuditService audit) {
        this.documents = documents;
        this.chunks = chunks;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
        this.index = index;
        this.audit = audit;
    }

    @PostConstruct
    void init() {
        reindex();
    }

    @Transactional
    public DocumentDto importPdf(String originalName, byte[] data, AccessLevel level, AppUser by) {
        if (!by.role().canUpload(level)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre rôle ne permet pas d'importer un document « " + level + " ».");
        }
        if (data.length < 5 || !new String(data, 0, 5).startsWith("%PDF")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les fichiers PDF sont acceptés.");
        }
        String sha = Hashing.sha256(data);
        if (documents.existsBySha256(sha)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce document est déjà dans la base (même empreinte SHA-256).");
        }
        List<String> pages = extractor.pages(data);
        List<Chunker.Piece> pieces = chunker.split(pages);
        if (pieces.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun texte trouvé dans ce PDF (document scanné ?).");
        }

        String name = cleanName(originalName);
        DocumentEntity doc = new DocumentEntity(name, storage.save(data), level, sha, by.username());
        doc.setPages(pages.size());
        doc.setChunks(pieces.size());
        documents.save(doc);

        List<ChunkEntity> rows = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            rows.add(new ChunkEntity(doc.getId(), pieces.get(i).page(), i, pieces.get(i).text()));
        }
        chunks.saveAll(rows);
        reindex();
        audit.log(by, "IMPORT", name + " (" + level + ", " + pages.size() + " pages, SHA-256 " + sha.substring(0, 12) + "…)");
        return DocumentDto.of(doc);
    }

    public List<DocumentDto> visibleTo(AppUser user) {
        return documents.findByAccessLevelInOrderByIdAsc(user.role().readableLevels()).stream().map(DocumentDto::of).toList();
    }

    /** Recalcule l'empreinte du fichier stocké et la compare à celle de l'import. */
    @Transactional
    public VerifyResult verify(Long id, AppUser by) {
        DocumentEntity d = documents.findById(id)
            .filter(x -> by.role().canRead(x.getAccessLevel()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
        return check(d, by, false);
    }

    @Transactional
    public List<VerifyResult> verifyAll(AppUser by) {
        return documents.findByAccessLevelInOrderByIdAsc(by.role().readableLevels()).stream().map(d -> check(d, by, false)).toList();
    }

    /** Vérification automatique planifiée : n'écrit dans le journal que les alertes. */
    @Transactional
    public long verifyAllQuietly() {
        return documents.findAll().stream().map(d -> check(d, AuditService.SYSTEM, true))
            .filter(r -> r.integrity() == Integrity.ALTERE).count();
    }

    private VerifyResult check(DocumentEntity d, AppUser by, boolean quiet) {
        String actual = storage.read(d.getStoredFile()).map(Hashing::sha256).orElse("fichier-manquant");
        Integrity result = Hashing.same(d.getSha256(), actual) ? Integrity.OK : Integrity.ALTERE;
        Integrity before = d.getIntegrity();
        d.markChecked(result);
        documents.save(d);
        if (result == Integrity.ALTERE && before != Integrity.ALTERE) {
            audit.log(by, "ALERTE_INTEGRITE", d.getName() + " : empreinte différente de l'original, document exclu des réponses");
            log.warn("Intégrité : le document {} ({}) a été modifié", d.getId(), d.getName());
        } else if (!quiet) {
            audit.log(by, "VERIFICATION", d.getName() + " : " + (result == Integrity.OK ? "intègre" : "toujours modifié"));
        }
        return new VerifyResult(d.getId(), result, d.getSha256(), actual);
    }

    @Transactional
    public void delete(Long id, AppUser by) {
        DocumentEntity d = documents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
        chunks.deleteByDocumentId(id);
        documents.delete(d);
        storage.delete(d.getStoredFile());
        reindex();
        audit.log(by, "SUPPRESSION", d.getName());
    }

    /** Documents modifiés : exclus des réponses tant qu'ils ne sont pas réimportés. */
    public Set<Long> alteredIds() {
        return documents.findAll().stream().filter(d -> d.getIntegrity() == Integrity.ALTERE)
            .map(DocumentEntity::getId).collect(Collectors.toSet());
    }

    public long count() {
        return documents.count();
    }

    public synchronized void reindex() {
        Map<Long, DocumentEntity> byId = new HashMap<>();
        documents.findAll().forEach(d -> byId.put(d.getId(), d));
        List<Bm25Index.Passage> passages = chunks.findAll().stream()
            .filter(c -> byId.containsKey(c.getDocumentId()))
            .map(c -> {
                DocumentEntity d = byId.get(c.getDocumentId());
                return new Bm25Index.Passage(c.getId(), d.getId(), d.getName(), d.getAccessLevel(), c.getPage(), c.getText());
            }).toList();
        index.rebuild(passages);
    }

    static String cleanName(String original) {
        String n = original == null || original.isBlank() ? "document.pdf" : original;
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._()-]", "_");
        return n.length() > 120 ? n.substring(0, 120) : n;
    }
}
