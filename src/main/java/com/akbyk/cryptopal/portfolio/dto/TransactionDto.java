package com.akbyk.cryptopal.portfolio.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionDto(
        UUID id,
        String assetSymbol,
        String transactionType,
        BigDecimal volume,
        BigDecimal executionPrice,
        OffsetDateTime executedAt
) {
}