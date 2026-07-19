package com.akbyk.cryptopal.ai.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiGenerateContentRequest {
    private List<GeminiContent> contents;
    private GeminiGenerationConfig generationConfig;
}