package com.akbyk.cryptopal.wallet;

import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import com.akbyk.cryptopal.wallet.dto.PortfolioResponseDto;
import com.akbyk.cryptopal.wallet.dto.WalletHoldingDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private static final String FIAT_SYMBOL = "USD";

    private final WalletBalanceRepository walletBalanceRepository;
    private final StringRedisTemplate redisTemplate;

    public WalletService(WalletBalanceRepository walletBalanceRepository,
                         StringRedisTemplate redisTemplate) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.redisTemplate = redisTemplate;
    }

    public PortfolioResponseDto getPortfolio(UUID userId) {
        List<WalletBalanceEntity> rows = walletBalanceRepository.findAllByUserId(userId);

        BigDecimal fiatBalance = BigDecimal.ZERO;
        List<WalletHoldingDto> holdings = new ArrayList<>();
        BigDecimal netWorth = BigDecimal.ZERO;
        boolean hasUnpricedHoldings = false;

        for (WalletBalanceEntity row : rows) {
            if (FIAT_SYMBOL.equals(row.getAssetSymbol())) {
                fiatBalance = row.getAmount();
                netWorth = netWorth.add(fiatBalance);
                continue;
            }

            // Skip zero-balance rows so a wallet row left over from a fully-sold
            // asset doesn't show up as a permanent "0 BTC" line in the UI.
            if (row.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal currentPrice = getCachedPrice(row.getAssetSymbol());
            BigDecimal valueUsd = null;
            if (currentPrice != null) {
                valueUsd = currentPrice.multiply(row.getAmount());
                netWorth = netWorth.add(valueUsd);
            } else {
                hasUnpricedHoldings = true;
            }

            holdings.add(new WalletHoldingDto(row.getAssetSymbol(), row.getAmount(), currentPrice, valueUsd));
        }

        return new PortfolioResponseDto(fiatBalance, holdings, netWorth, hasUnpricedHoldings);
    }

    private BigDecimal getCachedPrice(String symbol) {
        String raw = redisTemplate.opsForValue().get("price:" + symbol);
        return raw != null ? new BigDecimal(raw) : null;
    }
}
