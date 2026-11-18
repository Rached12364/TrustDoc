package com.trustdoc.chat;

import com.trustdoc.docs.DocumentService;
import com.trustdoc.llm.LlmGateway;
import com.trustdoc.search.Bm25Index;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    public record Status(String llmMode, String model, long documents, int chunks) {}

    private final LlmGateway llm;
    private final DocumentService documents;
    private final Bm25Index index;

    public StatusController(LlmGateway llm, DocumentService documents, Bm25Index index) {
        this.llm = llm;
        this.documents = documents;
        this.index = index;
    }

    @GetMapping
    public Status status() {
        String mode = llm.client().map(c -> c.available() ? c.name() : c.name() + "_off").orElse("extractif");
        return new Status(mode, llm.model(),
            documents.count(), index.size());
    }
}
