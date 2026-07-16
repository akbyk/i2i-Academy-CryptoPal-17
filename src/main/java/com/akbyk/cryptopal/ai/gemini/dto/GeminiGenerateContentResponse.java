package com.akbyk.cryptopal.ai.gemini.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeminiGenerateContentResponse {
    private List<GeminiCandidate> candidates;
}