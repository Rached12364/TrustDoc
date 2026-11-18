package com.trustdoc;

import static com.trustdoc.TestSupport.ask;
import static com.trustdoc.TestSupport.json;
import static com.trustdoc.TestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Tests de bout en bout de l'API, avec les 4 documents de démonstration et le mode extractif (sans LLM). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrustDocApiTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        TestSupport.tempStorage(r);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void refuse_un_mauvais_mot_de_passe() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"faux\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void api_inaccessible_sans_jeton() throws Exception {
        mvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"test\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void chaque_role_ne_voit_que_ses_documents() throws Exception {
        assertThat(documents(login(mvc, "employe", "employe123"))).hasSize(2);
        assertThat(documents(login(mvc, "rh", "rh123"))).hasSize(3);
        assertThat(documents(login(mvc, "admin", "admin123"))).hasSize(4);
    }

    @Test
    void repond_avec_la_source_et_la_page() throws Exception {
        JsonNode a = ask(mvc, login(mvc, "employe", "employe123"), "Combien de jours de télétravail par semaine ?");
        assertThat(a.get("grounded").asBoolean()).isTrue();
        assertThat(a.get("answer").asText()).contains("2 jours").contains("[1]");
        assertThat(a.get("sources").get(0).get("documentName").asText()).isEqualTo("guide_accueil.pdf");
        assertThat(a.get("sources").get(0).get("page").asInt()).isEqualTo(1);
    }

    @Test
    void un_collaborateur_n_obtient_jamais_une_information_rh() throws Exception {
        String question = "Quel est le délai pour déposer une note de frais ?";
        JsonNode employe = ask(mvc, login(mvc, "employe", "employe123"), question);
        assertThat(employe.get("grounded").asBoolean()).isFalse();
        assertThat(employe.get("answer").asText()).doesNotContain("30 jours");
        assertThat(employe.get("sources")).isEmpty();

        JsonNode rh = ask(mvc, login(mvc, "rh", "rh123"), question);
        assertThat(rh.get("grounded").asBoolean()).isTrue();
        assertThat(rh.get("answer").asText()).contains("30 jours");
        assertThat(rh.get("sources").get(0).get("documentName").asText()).isEqualTo("procedure_rh.pdf");
    }

    @Test
    void le_plan_confidentiel_reste_reserve_a_l_admin() throws Exception {
        String question = "En combien de temps les services critiques doivent-ils redémarrer ?";
        assertThat(ask(mvc, login(mvc, "rh", "rh123"), question).get("grounded").asBoolean()).isFalse();
        JsonNode admin = ask(mvc, login(mvc, "admin", "admin123"), question);
        assertThat(admin.get("answer").asText()).contains("4 heures");
    }

    @Test
    void refuse_de_repondre_hors_des_documents() throws Exception {
        JsonNode a = ask(mvc, login(mvc, "admin", "admin123"), "Quelle est la capitale du Japon ?");
        assertThat(a.get("grounded").asBoolean()).isFalse();
        assertThat(a.get("sources")).isEmpty();
    }

    @Test
    void droits_d_import_et_d_audit() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "x.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        mvc.perform(multipart("/api/documents").file(pdf).param("accessLevel", "PUBLIC")
            .header("Authorization", login(mvc, "employe", "employe123"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/documents").file(pdf).param("accessLevel", "CONFIDENTIEL")
            .header("Authorization", login(mvc, "rh", "rh123"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", login(mvc, "rh", "rh123"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", login(mvc, "admin", "admin123"))).andExpect(status().isOk());
    }

    @Test
    void refuse_un_fichier_qui_n_est_pas_un_pdf() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "x.pdf", "application/pdf", "bonjour".getBytes());
        mvc.perform(multipart("/api/documents").file(txt).param("accessLevel", "PUBLIC")
            .header("Authorization", login(mvc, "admin", "admin123"))).andExpect(status().isBadRequest());
    }

    private JsonNode documents(String token) throws Exception {
        return json(mvc.perform(get("/api/documents").header("Authorization", token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
