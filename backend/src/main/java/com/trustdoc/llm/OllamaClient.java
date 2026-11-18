package com.trustdoc.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Modèle local servi par Ollama (https://ollama.com) : les documents ne quittent jamais la machine. */
public class OllamaClient implements LlmClient {

    private final RestClient http;
    private final String model;
    private volatile long checkedAt;
    private volatile boolean up;

    public OllamaClient(RestClient.Builder builder, String baseUrl, String model) {
        this.http = builder.baseUrl(baseUrl).build();
        this.model = model;
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public String model() {
        return model;
    }

    /** Vérifie qu'Ollama tourne et que le modèle est téléchargé (résultat gardé 15 secondes). */
    @Override
    public boolean available() {
        long now = System.currentTimeMillis();
        if (now - checkedAt > 15_000) {
            try {
                JsonNode tags = http.get().uri("/api/tags").retrieve().body(JsonNode.class);
                boolean found = false;
                if (tags != null) {
                    for (JsonNode m : tags.path("models")) {
                        String name = m.path("name").asText();
                        found |= name.equals(model) || name.equals(model + ":latest");
                    }
                }
                up = found;
            } catch (RuntimeException e) {
                up = false;
            }
            checkedAt = now;
        }
        return up;
    }

    @Override
    public String complete(String system, String user) {
        Map<String, Object> body = Map.of(
            "model", model,
            "stream", false,
            "options", Map.of("temperature", 0.1),
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(body)
            .retrieve().body(JsonNode.class);
        if (res == null || res.path("message").path("content").isMissingNode()) {
            throw new IllegalStateException("Réponse Ollama vide");
        }
        return res.path("message").path("content").asText();
    }
}
