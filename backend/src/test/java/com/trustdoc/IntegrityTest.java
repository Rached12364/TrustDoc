package com.trustdoc;

import static com.trustdoc.TestSupport.ask;
import static com.trustdoc.TestSupport.json;
import static com.trustdoc.TestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.trustdoc.docs.DocumentEntity;
import com.trustdoc.docs.DocumentRepository;
import com.trustdoc.docs.FileStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Scénario complet : un fichier est modifié sur le disque → détecté, signalé, exclu des réponses. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrityTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        TestSupport.tempStorage(r);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    DocumentRepository documents;
    @Autowired
    FileStorage storage;

    @Test
    void detecte_un_document_modifie_et_l_exclut_des_reponses() throws Exception {
        String admin = login(mvc, "admin", "admin123");

        JsonNode before = json(mvc.perform(post("/api/documents/verify").header("Authorization", admin))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(before).allSatisfy(v -> assertThat(v.get("integrity").asText()).isEqualTo("OK"));

        String question = "Combien de jours de télétravail par semaine ?";
        assertThat(ask(mvc, admin, question).get("grounded").asBoolean()).isTrue();

        // Quelqu'un modifie le fichier du guide d'accueil directement sur le serveur
        DocumentEntity guide = documents.findAll().stream().filter(d -> d.getName().equals("guide_accueil.pdf")).findFirst().orElseThrow();
        Path file = storage.resolve(guide.getStoredFile());
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length / 2] ^= 0x01; // un seul bit change
        Files.write(file, bytes);

        JsonNode check = json(mvc.perform(post("/api/documents/" + guide.getId() + "/verify").header("Authorization", admin))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(check.get("integrity").asText()).isEqualTo("ALTERE");
        assertThat(check.get("actual").asText()).isNotEqualTo(check.get("expected").asText());

        // Le document modifié n'est plus utilisé pour répondre
        JsonNode after = ask(mvc, admin, question);
        assertThat(after.get("sources")).noneMatch(s -> s.get("documentName").asText().equals("guide_accueil.pdf"));

        // L'alerte est dans le journal d'audit
        JsonNode audit = json(mvc.perform(get("/api/audit").header("Authorization", admin))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(audit).anyMatch(e -> e.get("action").asText().equals("ALERTE_INTEGRITE"));
    }
}
