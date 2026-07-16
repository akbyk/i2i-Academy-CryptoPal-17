package com.akbyk.cryptopal.market.binance;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "cryptopal.binance")
public class BinanceProperties {
    private String baseUrl = "https://api.binance.com";
    private List<String> symbols = List.of("BTCUSDT", "ETHUSDT");
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
}