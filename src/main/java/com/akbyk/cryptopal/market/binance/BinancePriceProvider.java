package com.akbyk.cryptopal.market.binance;

import com.akbyk.cryptopal.market.PriceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "cryptopal.data-source", havingValue = "binance", matchIfMissing = true)
public class BinancePriceProvider implements PriceProvider {

    private final RestClient binanceRestClient;
    private final BinanceProperties properties;

    // Last successfully fetched prices, used as a fallback if Binance is
    // temporarily unreachable — keeps the cache from going stale to null.
    private final Map<String, BigDecimal> lastKnownPrices = new ConcurrentHashMap<>();

    public BinancePriceProvider(RestClient binanceRestClient, BinanceProperties properties) {
        this.binanceRestClient = binanceRestClient;
        this.properties = properties;
    }

    @Override
    public Map<String, BigDecimal> fetchLatestPrices() {
        try {
            String symbolsParam = properties.getSymbols().stream()
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(",", "[", "]"));

            // Replace the .uri(...) block in BinancePriceProvider.java with this:
            List<BinanceTickerPriceResponse> response = binanceRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/ticker/price")
                            // Use a template string to prevent double encoding of brackets & quotes
                            .query("symbols={symbols}")
                            .build(symbolsParam)) // Binds the raw string directly
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BinanceTickerPriceResponse>>() {});

            if (response == null || response.isEmpty()) {
                log.warn("Binance returned an empty price list; falling back to last known prices.");
                return Map.copyOf(lastKnownPrices);
            }

            Map<String, BigDecimal> prices = response.stream()
                    .collect(Collectors.toMap(
                            r -> toInternalSymbol(r.getSymbol()),
                            r -> new BigDecimal(r.getPrice())
                    ));

            lastKnownPrices.putAll(prices);
            return prices;

        } catch (Exception ex) {
            // Any Binance failure (timeout, 5xx, malformed body) must never propagate
            // up and break the scheduler tick — fall back to whatever we saw last.
            log.error("Failed to fetch prices from Binance; falling back to last known prices.", ex);
            return Map.copyOf(lastKnownPrices);
        }
    }

    private String toInternalSymbol(String binanceSymbol) {
        // BTCUSDT -> BTC, ETHUSDT -> ETH
        return binanceSymbol.replaceAll("USDT$", "");
    }
}