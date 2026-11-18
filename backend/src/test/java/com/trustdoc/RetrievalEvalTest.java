package com.trustdoc;

import static com.trustdoc.TestSupport.ask;
import static com.trustdoc.TestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Évaluation : un jeu de questions avec la réponse attendue (document, page, mot-clé).
 * On mesure la part de questions où la 1re source est le bon document ET où la réponse contient le fait attendu,
 * et on vérifie que les questions hors sujet sont bien refusées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RetrievalEvalTest {

    record Case(String question, String document, int page, String fact) {}

    static final List<Case> CASES = List.of(
        new Case("Combien de jours de télétravail par semaine ?", "guide_accueil.pdf", 1, "2 jours"),
        new Case("À quelle heure a lieu la réunion d'équipe ?", "guide_accueil.pdf", 1, "9 h 30"),
        new Case("Quels sont les horaires de présence obligatoire ?", "guide_accueil.pdf", 1, "10 h à 16 h"),
        new Case("Quel est le numéro de poste du helpdesk ?", "guide_accueil.pdf", 2, "4400"),
        new Case("Que faire si je perds mon badge ?", "guide_accueil.pdf", 2, "24 heures"),
        new Case("Quelle est la longueur minimale d'un mot de passe ?", "politique_securite.pdf", 1, "12 caractères"),
        new Case("La double authentification est-elle obligatoire ?", "politique_securite.pdf", 1, "obligatoire"),
        new Case("Que faire si je reçois un e-mail suspect ?", "politique_securite.pdf", 2, "Signaler"),
        new Case("Puis-je utiliser un Wi-Fi public ?", "politique_securite.pdf", 2, "VPN"),
        new Case("Combien de jours de congés payés par an ?", "procedure_rh.pdf", 1, "22 jours"),
        new Case("Combien de temps avant le départ faut-il demander un congé ?", "procedure_rh.pdf", 1, "15 jours"),
        new Case("Combien de jours de congé pour un mariage ?", "procedure_rh.pdf", 1, "3 jours"),
        new Case("Quel est le plafond pour un repas d'affaires ?", "procedure_rh.pdf", 2, "60 dinars"),
        new Case("Quelle est la perte de données maximale acceptée ?", "plan_continuite.pdf", 1, "1 heure"),
        new Case("Quand a lieu le test de bascule vers le site de secours ?", "plan_continuite.pdf", 1, "mars"),
        new Case("Qui fait partie de la cellule de crise ?", "plan_continuite.pdf", 2, "directeur général"));

    static final List<String> OFF_TOPIC = List.of(
        "Quelle est la capitale du Japon ?",
        "Qui a gagné la Coupe du monde de football ?",
        "Donne-moi une recette de couscous.");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        TestSupport.tempStorage(r);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void evaluation_de_la_recherche_et_des_refus() throws Exception {
        String admin = login(mvc, "admin", "admin123");
        int docOk = 0;
        int pageOk = 0;
        int factOk = 0;
        StringBuilder report = new StringBuilder("\n=== Évaluation TrustDoc ===\n");
        for (Case c : CASES) {
            JsonNode a = ask(mvc, admin, c.question());
            JsonNode first = a.get("sources").path(0);
            boolean doc = c.document().equals(first.path("documentName").asText());
            boolean page = doc && first.path("page").asInt() == c.page();
            boolean fact = a.get("answer").asText().contains(c.fact());
            docOk += doc ? 1 : 0;
            pageOk += page ? 1 : 0;
            factOk += fact ? 1 : 0;
            report.append(String.format("%s %s %s  %s%n", doc ? "doc✓" : "doc✗", page ? "page✓" : "page✗", fact ? "fait✓" : "fait✗", c.question()));
        }
        int refused = 0;
        for (String q : OFF_TOPIC) {
            refused += ask(mvc, admin, q).get("grounded").asBoolean() ? 0 : 1;
        }
        int n = CASES.size();
        report.append(String.format("Bon document : %d/%d · bonne page : %d/%d · fait présent : %d/%d · hors sujet refusés : %d/%d%n",
            docOk, n, pageOk, n, factOk, n, refused, OFF_TOPIC.size()));
        System.out.println(report);

        assertThat(docOk).as("bon document en 1re source").isGreaterThanOrEqualTo((int) Math.ceil(n * 0.9));
        assertThat(factOk).as("fait attendu dans la réponse").isGreaterThanOrEqualTo((int) Math.ceil(n * 0.8));
        assertThat(refused).as("questions hors sujet refusées").isEqualTo(OFF_TOPIC.size());
    }
}
