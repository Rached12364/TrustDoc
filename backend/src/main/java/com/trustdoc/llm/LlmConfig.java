package com.trustdoc.llm;

import com.trustdoc.config.TrustDocProperties;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Choisit le modèle selon trustdoc.llm.mode : ollama (par défaut), openai, ou extractif (aucun LLM). */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    LlmGateway llmGateway(TrustDocProperties props) {
        return new LlmGateway(build(props));
    }

    private Optional<LlmClient> build(TrustDocProperties props) {
        var llm = props.llm();
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(llm.timeoutSeconds()));
        RestClient.Builder builder = RestClient.builder().requestFactory(ClientHttpRequestFactories.get(settings));
        String mode = llm.mode() == null ? "ollama" : llm.mode().trim().toLowerCase();
        return switch (mode) {
            case "openai" -> {
                if (llm.openai().apiKey() == null || llm.openai().apiKey().isBlank()) {
                    log.warn("Mode openai sans clé (OPENAI_API_KEY) : passage en mode extractif");
                    yield Optional.empty();
                }
                log.info("LLM : API compatible OpenAI {} ({})", llm.openai().url(), llm.openai().model());
                yield Optional.of(new OpenAiClient(builder, llm.openai().url(), llm.openai().model(), llm.openai().apiKey()));
            }
            case "extractif" -> {
                log.info("LLM : désactivé (mode extractif)");
                yield Optional.empty();
            }
            default -> {
                log.info("LLM : Ollama local {} ({})", llm.ollama().url(), llm.ollama().model());
                yield Optional.of(new OllamaClient(builder, llm.ollama().url(), llm.ollama().model()));
            }
        };
    }
}
