package com.akbyk.cryptopal.ai.gemini;

import com.akbyk.cryptopal.ai.gemini.dto.GeminiContent;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentRequest;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiGenerateContentResponse;
import com.akbyk.cryptopal.ai.gemini.dto.GeminiPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
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
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))));

        try {
            // Base URL'in sonundaki slaş durumunu garantiye alalım
            String base = properties.getBaseUrl();
            if (base != null && !base.endsWith("/")) {
                base = base + "/";
            }

            // cURL ile attığımız ham URL'in birebir aynısını oluşturuyoruz
            String rawUrl = String.format("%sv1beta/models/%s:generateContent?key=%s",
                    base, properties.getModel(), properties.getApiKey());

            log.info("Sending strictly unencoded URI to Google: {}", rawUrl);

            // URI.create otomatik encoding'i tamamen bypass eder, ham string neyse onu iletir.
            URI strictlyUnencodedUri = URI.create(rawUrl);

            GeminiGenerateContentResponse response = geminiWebClient.post()
                    .uri(strictlyUnencodedUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiGenerateContentResponse.class)
                    .timeout(Duration.ofSeconds(30L))
                    .onErrorResume(ex -> {
                        log.error("Gemini API call failed during Mono execution", ex);
                        return Mono.empty();
                    })
                    .block(Duration.ofSeconds(35L));

            if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
                return Optional.empty();
            }

            return response.getCandidates().get(0).getContent().getParts().stream()
                    .map(GeminiPart::getText)
                    .findFirst();

        } catch (Exception ex) {
            log.error("Unexpected error inside GeminiClient", ex);
            return Optional.empty();
        }
    }
}