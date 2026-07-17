package com.akbyk.cryptopal.ai.gemini;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClientConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}