package com.trustdoc.docs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Vérifie automatiquement l'intégrité de tous les documents (par défaut toutes les 10 minutes). */
@Component
public class IntegrityScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntegrityScheduler.class);

    private final DocumentService documents;

    public IntegrityScheduler(DocumentService documents) {
        this.documents = documents;
    }

    @Scheduled(cron = "${trustdoc.integrity.cron}")
    public void run() {
        long altered = documents.verifyAllQuietly();
        if (altered > 0) {
            log.warn("Vérification planifiée : {} document(s) modifié(s)", altered);
        }
    }
}
