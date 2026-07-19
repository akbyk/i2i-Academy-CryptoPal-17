package com.akbyk.cryptopal.wallet.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full portfolio snapshot for the authenticated user: cash balance, every
 * non-fiat holding (with live valuation where price data is available), and
 * total net worth. totalNetWorth only sums holdings whose current price is
 * known — see hasUnpricedHoldings if you need to warn the user that the
 * total may be incomplete.
 */
public record PortfolioResponseDto(
        BigDecimal fiatBalance,
        List<WalletHoldingDto> holdings,
        BigDecimal totalNetWorth,
        boolean hasUnpricedHoldings
) {
}
