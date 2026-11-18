package com.trustdoc.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.trustdoc.audit.AuditService;
import com.trustdoc.auth.AppUser;
import com.trustdoc.common.AccessLevel;
import com.trustdoc.common.Role;
import com.trustdoc.config.TrustDocProperties;
import com.trustdoc.docs.DocumentService;
import com.trustdoc.llm.LlmClient;
import com.trustdoc.llm.LlmGateway;
import com.trustdoc.llm.OllamaClient;
import com.trustdoc.search.Bm25Index;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Le chemin « avec LLM » : citations vérifiées, refus du modèle, panne du modèle, format de l'appel Ollama. */
class LlmFlowTest {

    private final Bm25Index index = new Bm25Index();
    private final DocumentService documents = mock(DocumentService.class);
    private final AuditService audit = mock(AuditService.class);
    private final AppUser employe = new AppUser("employe", "Sami", Role.EMPLOYE);
    private final TrustDocProperties props = new TrustDocProperties(null, null, null,
        new TrustDocProperties.Search(4, 0.5), null);

    @BeforeEach
    void setUp() {
        when(documents.alteredIds()).thenReturn(Set.of());
        index.rebuild(List.of(
            new Bm25Index.Passage(1, 10, "guide_accueil.pdf", AccessLevel.PUBLIC, 1,
                "Télétravail. Chaque collaborateur peut faire jusqu'à 2 jours de télétravail par semaine, après la période d'essai."),
            new Bm25Index.Passage(2, 20, "procedure_rh.pdf", AccessLevel.RH, 2,
                "Les notes de frais doivent être déposées dans un délai de 30 jours après la dépense.")));
    }

    private ChatService service(BiFunction<String, String, String> model) {
        LlmClient client = new LlmClient() {
            public String name() { return "ollama"; }
            public String model() { return "test"; }
            public String complete(String system, String user) { return model.apply(system, user); }
        };
        return new ChatService(index, documents, new LlmGateway(Optional.of(client)), audit, props);
    }

    @Test
    void reponse_du_modele_avec_citation_valide() {
        var a = service((s, u) -> "Vous pouvez faire jusqu'à 2 jours de télétravail par semaine [1].")
            .ask("Combien de jours de télétravail ?", employe);
        assertThat(a.grounded()).isTrue();
        assertThat(a.mode()).isEqualTo("ollama");
        assertThat(a.answer()).endsWith("[1].");
    }

    @Test
    void le_prompt_ne_contient_que_les_passages_autorises() {
        AtomicReference<String> prompt = new AtomicReference<>();
        service((s, u) -> { prompt.set(u); return "2 jours [1]."; }).ask("télétravail par semaine et notes de frais", employe);
        assertThat(prompt.get()).contains("guide_accueil.pdf").doesNotContain("procedure_rh.pdf").doesNotContain("30 jours");
    }

    @Test
    void reponse_sans_citation_remplacee_par_une_reponse_extractive() {
        var a = service((s, u) -> "Je pense que c'est 3 jours.").ask("Combien de jours de télétravail par semaine ?", employe);
        assertThat(a.mode()).isEqualTo("extractif");
        assertThat(a.answer()).contains("2 jours").contains("[1]").doesNotContain("3 jours");
    }

    @Test
    void le_modele_peut_dire_qu_il_ne_sait_pas() {
        var a = service((s, u) -> ChatService.UNKNOWN_TOKEN).ask("Combien de jours de télétravail par semaine ?", employe);
        assertThat(a.grounded()).isFalse();
        assertThat(a.answer()).isEqualTo(ChatService.REFUSAL);
    }

    @Test
    void modele_en_panne_reponse_extractive() {
        var a = service((s, u) -> { throw new IllegalStateException("connexion refusée"); })
            .ask("Combien de jours de télétravail par semaine ?", employe);
        assertThat(a.grounded()).isTrue();
        assertThat(a.mode()).isEqualTo("extractif");
    }

    @Test
    void client_ollama_envoie_le_bon_format_et_lit_la_reponse() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", ex -> {
            received.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"message\":{\"role\":\"assistant\",\"content\":\"Deux jours [1].\"},\"done\":true}"
                .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            String out = new OllamaClient(RestClient.builder(), url, "llama3.2:3b").complete("consignes", "question");
            assertThat(out).isEqualTo("Deux jours [1].");
            assertThat(received.get()).contains("\"model\":\"llama3.2:3b\"").contains("\"stream\":false").contains("consignes");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appel_vers_un_ollama_absent_ne_bloque_pas() {
        LlmClient dead = new OllamaClient(RestClient.builder(), "http://127.0.0.1:1", "x");
        var svc = new ChatService(index, documents, new LlmGateway(Optional.of(dead)), audit, props);
        assertThat(svc.ask("Combien de jours de télétravail par semaine ?", employe).mode()).isEqualTo("extractif");
    }
}
