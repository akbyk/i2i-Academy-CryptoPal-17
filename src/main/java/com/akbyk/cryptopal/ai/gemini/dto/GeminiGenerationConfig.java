package com.akbyk.cryptopal.ai.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerationConfig {

    @JsonProperty("maxOutputTokens")
    private Integer maxOutputTokens;

    private Double temperature;
}