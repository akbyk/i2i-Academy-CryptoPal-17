package com.akbyk.cryptopal.ai;

import com.akbyk.cryptopal.ai.dto.AiQueryRequestDto;
import com.akbyk.cryptopal.ai.dto.AiQueryResponseDto;
import com.akbyk.cryptopal.ai.gemini.GeminiClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AiService {

    private final AiContextService contextService;
    private final GeminiClient geminiClient;
    private final AiRateLimiterService rateLimiterService;

    public AiService(AiContextService contextService, GeminiClient geminiClient,
                     AiRateLimiterService rateLimiterService) {
        this.contextService = contextService;
        this.geminiClient = geminiClient;
        this.rateLimiterService = rateLimiterService;
    }

    public AiQueryResponseDto query(UUID userId, AiQueryRequestDto request) {
        rateLimiterService.checkAndRecord(userId);

        String prompt = contextService.buildContextPrompt(userId, request.getMessage());
        Optional<String> result = geminiClient.generateContent(prompt);

        if (result.isEmpty()) {
            return new AiQueryResponseDto(null, "AI service temporarily unavailable");
        }

        String responseText = result.get();
        contextService.appendHistory(userId, request.getMessage(), responseText);
        return new AiQueryResponseDto(responseText, null);
    }
}