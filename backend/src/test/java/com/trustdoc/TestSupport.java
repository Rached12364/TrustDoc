package com.trustdoc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;

/** Outils communs aux tests d'intégration : dossier de stockage temporaire, connexion, question. */
public final class TestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TestSupport() {}

    public static void tempStorage(DynamicPropertyRegistry r) {
        try {
            Path dir = Files.createTempDirectory("trustdoc-test");
            r.add("trustdoc.storage.dir", dir::toString);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String login(MockMvc mvc, String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("username", user, "password", pass))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return "Bearer " + JSON.readTree(body).get("token").asText();
    }

    public static JsonNode ask(MockMvc mvc, String token, String question) throws Exception {
        String body = mvc.perform(post("/api/chat").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("question", question))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JSON.readTree(body);
    }

    public static JsonNode json(String body) throws IOException {
        return JSON.readTree(body);
    }
}
