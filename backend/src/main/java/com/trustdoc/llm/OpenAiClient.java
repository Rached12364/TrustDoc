package com.trustdoc.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Toute API compatible OpenAI (OpenAI, Groq, Mistral, vLLM...). Option : les passages envoyés quittent alors le serveur. */
public class OpenAiClient implements LlmClient {

    private final RestClient http;
    private final String model;

    public OpenAiClient(RestClient.Builder builder, String baseUrl, String model, String apiKey) {
        this.http = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Bearer " + apiKey).build();
        this.model = model;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String complete(String system, String user) {
        Map<String, Object> body = Map.of(
            "model", model,
            "temperature", 0.1,
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON).body(body)
            .retrieve().body(JsonNode.class);
        JsonNode content = res == null ? null : res.path("choices").path(0).path("message").path("content");
        if (content == null || content.isMissingNode()) {
            throw new IllegalStateException("Réponse de l'API vide");
        }
        return content.asText();
    }
}
