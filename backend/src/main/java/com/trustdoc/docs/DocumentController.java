package com.trustdoc.docs;

import com.trustdoc.auth.CurrentUser;
import com.trustdoc.common.AccessLevel;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentDto> list() {
        return service.visibleTo(CurrentUser.get());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RH')")
    public ResponseEntity<DocumentDto> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "PUBLIC") AccessLevel accessLevel) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide.");
        }
        DocumentDto doc = service.importPdf(file.getOriginalFilename(), file.getBytes(), accessLevel, CurrentUser.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @PostMapping("/{id}/verify")
    public VerifyResult verify(@PathVariable Long id) {
        return service.verify(id, CurrentUser.get());
    }

    @PostMapping("/verify")
    public List<VerifyResult> verifyAll() {
        return service.verifyAll(CurrentUser.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, CurrentUser.get());
        return ResponseEntity.noContent().build();
    }
}
