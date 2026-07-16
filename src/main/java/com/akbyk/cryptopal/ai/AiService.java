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

    public AiService(AiContextService contextService, GeminiClient geminiClient) {
        this.contextService = contextService;
        this.geminiClient = geminiClient;
    }

    public AiQueryResponseDto query(UUID userId, AiQueryRequestDto request) {
        String prompt = contextService.buildContextPrompt(userId, request.getMessage());
        Optional<String> result = geminiClient.generateContent(prompt);

        return result
                .map(text -> new AiQueryResponseDto(text, null))
                .orElseGet(() -> new AiQueryResponseDto(null, "AI service temporarily unavailable"));
    }
}