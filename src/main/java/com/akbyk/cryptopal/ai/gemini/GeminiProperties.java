package com.akbyk.cryptopal.ai.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.gemini")
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-flash-lite-latest";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int timeoutSeconds = 30;

    private int maxOutputTokens = 1024;
    private double temperature = 0.3;
}