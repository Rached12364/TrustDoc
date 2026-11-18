package com.trustdoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Toutes les options « trustdoc.* » de application.properties. */
@ConfigurationProperties(prefix = "trustdoc")
public record TrustDocProperties(Storage storage, Demo demo, Jwt jwt, Search search, Llm llm) {

    public record Storage(String dir) {}

    public record Demo(boolean load) {}

    public record Jwt(String secret, long ttlMinutes) {}

    public record Search(int topK, double minCoverage) {}

    public record Llm(String mode, int timeoutSeconds, Ollama ollama, OpenAi openai) {}

    public record Ollama(String url, String model) {}

    public record OpenAi(String url, String model, String apiKey) {}
}
