package com.akbyk.cryptopal.market.binance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BinancePriceProviderTest {

    private MockRestServiceServer mockServer;
    private BinancePriceProvider provider;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties();
        properties.setBaseUrl("https://api.binance.com");
        properties.setSymbols(List.of("BTCUSDT", "ETHUSDT"));

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
        mockServer = MockRestServiceServer.bindTo(builder).build();

        provider = new BinancePriceProvider(builder.build(), properties);
    }

    @Test
    void fetchLatestPrices_parsesAndMapsBinanceSymbolsToInternalSymbols() {
        String body = """
                [
                  {"symbol":"BTCUSDT","price":"67123.45000000"},
                  {"symbol":"ETHUSDT","price":"3456.78000000"}
                ]
                """;

        mockServer.expect(method(GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, BigDecimal> prices = provider.fetchLatestPrices();

        assertThat(prices).containsKeys("BTC", "ETH");
        assertThat(prices.get("BTC")).isEqualByComparingTo("67123.45000000");
        assertThat(prices.get("ETH")).isEqualByComparingTo("3456.78000000");
    }

    @Test
    void fetchLatestPrices_fallsBackToLastKnownPricesOnFailure() {
        String body = """
                [{"symbol":"BTCUSDT","price":"67000.00000000"}]
                """;
        mockServer.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        Map<String, BigDecimal> first = provider.fetchLatestPrices();
        assertThat(first.get("BTC")).isEqualByComparingTo("67000.00000000");

        mockServer.reset();
        mockServer.expect(method(GET)).andRespond(request -> {
            throw new java.io.IOException("simulated network failure");
        });

        Map<String, BigDecimal> second = provider.fetchLatestPrices();
        assertThat(second.get("BTC")).isEqualByComparingTo("67000.00000000");
    }
}