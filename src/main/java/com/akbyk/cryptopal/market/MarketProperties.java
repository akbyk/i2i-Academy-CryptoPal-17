package com.akbyk.cryptopal.market;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "cryptopal.market")
public class MarketProperties {
    private List<String> trackedSymbols = List.of("BTC", "ETH");
}