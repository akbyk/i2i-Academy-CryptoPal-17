package com.akbyk.cryptopal.portfolio.dto;

import java.math.BigDecimal;

public record BalanceDto(String assetSymbol, BigDecimal amount) {
}