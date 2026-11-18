package com.trustdoc.llm;

import java.util.Optional;

/** Le modèle configuré, ou vide en mode extractif (sans LLM). */
public record LlmGateway(Optional<LlmClient> client) {

    public String mode() {
        return client.map(LlmClient::name).orElse("extractif");
    }

    public String model() {
        return client.map(LlmClient::model).orElse("aucun");
    }
}
