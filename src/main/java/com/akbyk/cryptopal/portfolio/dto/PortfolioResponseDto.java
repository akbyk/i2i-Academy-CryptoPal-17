package com.akbyk.cryptopal.portfolio.dto;

import java.util.List;

public record PortfolioResponseDto(List<BalanceDto> balances, List<TransactionDto> transactions) {
}