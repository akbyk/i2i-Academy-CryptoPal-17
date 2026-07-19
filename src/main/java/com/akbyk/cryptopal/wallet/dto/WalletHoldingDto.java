package com.akbyk.cryptopal.wallet.dto;

import java.math.BigDecimal;

/**
 * A single non-fiat holding in the user's wallet, valued at the latest
 * Redis-cached price. currentPrice/valueUsd are null when the price cache
 * hasn't warmed up yet for this symbol (never blocks the response).
 */
public record WalletHoldingDto(
        String assetSymbol,
        BigDecimal amount,
        BigDecimal currentPrice,
        BigDecimal valueUsd
) {
}
