package com.trustdoc.docs;

import com.trustdoc.audit.AuditService;
import com.trustdoc.common.AccessLevel;
import com.trustdoc.config.TrustDocProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Au premier démarrage (base vide), importe 4 documents fictifs de l'entreprise « Orbis Services ». */
@Component
public class DemoLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoLoader.class);

    static final Map<String, AccessLevel> DEMO = new LinkedHashMap<>();

    static {
        DEMO.put("guide_accueil.pdf", AccessLevel.PUBLIC);
        DEMO.put("politique_securite.pdf", AccessLevel.PUBLIC);
        DEMO.put("procedure_rh.pdf", AccessLevel.RH);
        DEMO.put("plan_continuite.pdf", AccessLevel.CONFIDENTIEL);
    }

    private final DocumentService documents;
    private final TrustDocProperties props;

    public DemoLoader(DocumentService documents, TrustDocProperties props) {
        this.documents = documents;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!props.demo().load() || documents.count() > 0) {
            return;
        }
        for (Map.Entry<String, AccessLevel> e : DEMO.entrySet()) {
            try (InputStream in = new ClassPathResource("demo/" + e.getKey()).getInputStream()) {
                documents.importPdf(e.getKey(), in.readAllBytes(), e.getValue(), AuditService.SYSTEM);
            }
        }
        log.info("TrustDoc : {} documents de démonstration importés", DEMO.size());
    }
}
