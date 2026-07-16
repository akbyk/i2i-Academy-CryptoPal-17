package com.akbyk.cryptopal.ai;

import com.akbyk.cryptopal.ai.dto.AiQueryRequestDto;
import com.akbyk.cryptopal.ai.dto.AiQueryResponseDto;
import com.akbyk.cryptopal.auth.AuthenticatedUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Insights")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/query")
    @Operation(summary = "Ask the AI assistant a question about your portfolio, with live context")
    public ResponseEntity<AiQueryResponseDto> query(@Valid @RequestBody AiQueryRequestDto request) {
        UUID userId = AuthenticatedUserContext.getCurrentUserId();
        return ResponseEntity.ok(aiService.query(userId, request));
    }
}