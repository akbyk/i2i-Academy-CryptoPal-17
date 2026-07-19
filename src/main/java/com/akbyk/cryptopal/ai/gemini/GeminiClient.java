package com.akbyk.cryptopal.ai.gemini;

import com.akbyk.cryptopal.ai.gemini.dto.GeminiContent;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentRequest;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentResponse;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerationConfig;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
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

    public Optional<String> generateContent(String prompt) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))),
                new GeminiGenerationConfig(properties.getMaxOutputTokens(), properties.getTemperature()));

        try {
            GeminiGenerateContentResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.getApiKey())
                            .build(properties.getModel()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiGenerateContentResponse.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
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
            log.error("Unexpected error calling Gemini API", ex);
            return Optional.empty();
        }
    }
}