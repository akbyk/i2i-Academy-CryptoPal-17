package com.akbyk.cryptopal.market;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceDto(String symbol, BigDecimal price, Instant timestamp) {
}