package com.akbyk.cryptopal.ai.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-flash-lite-latest"; //TODO redundant
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int timeoutSeconds = 10;
}
