package com.akbyk.cryptopal;

import com.akbyk.cryptopal.ai.gemini.GeminiProperties;
import com.akbyk.cryptopal.market.MarketProperties;
import com.akbyk.cryptopal.market.binance.BinanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BinanceProperties.class, MarketProperties.class, GeminiProperties.class})
public class CryptoPalApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoPalApplication.class, args);
    }
}