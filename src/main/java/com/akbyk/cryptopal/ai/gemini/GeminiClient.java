package com.akbyk.cryptopal.ai.gemini;

import com.akbyk.cryptopal.ai.gemini.dto.GeminiContent;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentRequest;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentResponse;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerationConfig;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiPart;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GeminiClient {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;

    public GeminiClient(WebClient geminiWebClient, GeminiProperties properties) {
        this.geminiWebClient = geminiWebClient;
        this.properties = properties;
    }

    @PostConstruct
    public void verifyConfig() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Gemini API key is missing from configuration!");
        }
    }

    /**
     * Returns Optional.empty() on ANY failure (timeout, network error, malformed
     * response, invalid API key) — callers never see a raw exception or the API key.
     *
     * Blocking justification: this app is a classic Spring MVC (servlet) application,
     * not an end-to-end reactive one. Every other step in the request this method is
     * called from (fetching wallet balances, transactions, and price_trend_log rows
     * via Spring Data JPA) is already blocking JDBC I/O on this same servlet thread.
     * Making only this one call non-blocking would not free the thread for reuse —
     * it is still pinned by the blocking DB calls immediately before and after it —
     * so a fully reactive chain here would add complexity with no real concurrency
     * benefit. A bounded .block(timeout) keeps the method simple to reason about while
     * still guaranteeing the servlet thread is never held indefinitely: the reactive
     * .timeout() below fires first during normal operation, and the outer .block()
     * timeout is a hard backstop in case anything upstream misbehaves.
     */
    public Optional<String> generateContent(String prompt) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
                new GeminiGenerationConfig(properties.getMaxOutputTokens(), properties.getTemperature()));

        try {
            GeminiGenerateContentResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.getApiKey())
                            .build(Map.of("model", properties.getModel())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiGenerateContentResponse.class)                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .onErrorResume(ex -> {
                        log.error("Gemini API call failed or timed out", ex);
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds() + 1L));

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                return Optional.empty();
            }

            return response.getCandidates().get(0).getContent().getParts().stream()
                    .map(GeminiPart::getText)
                    .findFirst();

        } catch (Exception ex) {
            // Final safety net — never let a raw exception (which could include
            // headers/URL fragments) propagate up to the controller.
            log.error("Unexpected error calling Gemini API", ex);
            return Optional.empty();
        }
    }
}