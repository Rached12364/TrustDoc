package com.trustdoc.docs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Un passage d'un document (quelques phrases), avec sa page : c'est l'unité que l'on recherche et que l'on cite. */
@Entity
@Table(name = "chunk", indexes = @Index(columnList = "documentId"))
public class ChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;
    private int page;
    private int position;

    @Column(length = 4000)
    private String text;

    protected ChunkEntity() {}

    public ChunkEntity(Long documentId, int page, int position, String text) {
        this.documentId = documentId;
        this.page = page;
        this.position = position;
        this.text = text;
    }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public int getPage() { return page; }
    public int getPosition() { return position; }
    public String getText() { return text; }
}
