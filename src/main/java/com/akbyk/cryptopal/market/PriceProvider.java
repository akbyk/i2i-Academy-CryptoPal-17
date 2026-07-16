package com.akbyk.cryptopal.market;

import java.math.BigDecimal;
import java.util.Map;

/**
 *
 * Swapping BinancePriceProvider for a different source requires no changes outside
 * the market.binance package
 */
public interface PriceProvider {
    Map<String, BigDecimal> fetchLatestPrices();
}