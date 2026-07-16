package com.akbyk.cryptopal.trading.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeResponseDto(
        UUID transactionId,
        String assetSymbol,
        String type,
        BigDecimal volume,
        BigDecimal executionPrice,
        BigDecimal updatedFiatBalance,
        BigDecimal updatedAssetBalance
) {
}