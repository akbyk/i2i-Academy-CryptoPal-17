package com.akbyk.cryptopal.trading;

import com.akbyk.cryptopal.common.entity.TransactionEntity;
import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import com.akbyk.cryptopal.common.repository.jpa.TransactionRepository;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import com.akbyk.cryptopal.trading.dto.TradeRequestDto;
import com.akbyk.cryptopal.trading.dto.TradeResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TradingServiceTest {

    private final WalletBalanceRepository walletBalanceRepository = mock(WalletBalanceRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private final TradingService tradingService =
            new TradingService(walletBalanceRepository, transactionRepository, redisTemplate);

    private final UUID userId = UUID.randomUUID();

    private void givenPrice(String symbol, String price) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("price:" + symbol)).thenReturn(price);
    }

    private WalletBalanceEntity wallet(String symbol, String amount) {
        WalletBalanceEntity w = new WalletBalanceEntity();
        w.setUserId(userId);
        w.setAssetSymbol(symbol);
        w.setAmount(new BigDecimal(amount));
        return w;
    }

    @Test
    void buy_deductsFiatAndCreditsAsset_whenBalanceSufficient() {
        givenPrice("BTC", "50000.00000000");

        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet("USD", "10000.00")));
        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "BTC"))
                .thenReturn(Optional.of(wallet("BTC", "0")));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> {
                    TransactionEntity tx = inv.getArgument(0);
                    tx.setId(UUID.randomUUID());
                    return tx;
                });

        TradeRequestDto request = new TradeRequestDto();
        request.setAssetSymbol("BTC");
        request.setType(TradeType.BUY);
        request.setVolume(new BigDecimal("0.1"));

        TradeResponseDto response = tradingService.executeTrade(userId, request);

        assertThat(response.updatedFiatBalance()).isEqualByComparingTo("5000.00");
        assertThat(response.updatedAssetBalance()).isEqualByComparingTo("0.1");
        verify(walletBalanceRepository, times(2)).save(any(WalletBalanceEntity.class));
    }

    @Test
    void buy_throwsInsufficientFunds_whenFiatBalanceTooLow() {
        givenPrice("BTC", "50000.00000000");

        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet("USD", "100.00")));
        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "BTC"))
                .thenReturn(Optional.of(wallet("BTC", "0")));

        TradeRequestDto request = new TradeRequestDto();
        request.setAssetSymbol("BTC");
        request.setType(TradeType.BUY);
        request.setVolume(new BigDecimal("1"));

        assertThrows(InsufficientFundsException.class, () -> tradingService.executeTrade(userId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void sell_throwsInsufficientFunds_whenAssetVolumeTooLow() {
        givenPrice("BTC", "50000.00000000");

        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "USD"))
                .thenReturn(Optional.of(wallet("USD", "1000.00")));
        when(walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, "BTC"))
                .thenReturn(Optional.of(wallet("BTC", "0.01")));

        TradeRequestDto request = new TradeRequestDto();
        request.setAssetSymbol("BTC");
        request.setType(TradeType.SELL);
        request.setVolume(new BigDecimal("1"));

        assertThrows(InsufficientFundsException.class, () -> tradingService.executeTrade(userId, request));
    }

    @Test
    void executeTrade_throwsAssetNotFound_whenRedisCacheCold() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("price:DOGE")).thenReturn(null);

        TradeRequestDto request = new TradeRequestDto();
        request.setAssetSymbol("DOGE");
        request.setType(TradeType.BUY);
        request.setVolume(new BigDecimal("1"));

        assertThrows(AssetNotFoundException.class, () -> tradingService.executeTrade(userId, request));
    }
}