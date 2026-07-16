package com.akbyk.cryptopal.trading;

import com.akbyk.cryptopal.common.entity.TransactionEntity;
import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import com.akbyk.cryptopal.common.repository.jpa.TransactionRepository;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import com.akbyk.cryptopal.trading.dto.TradeRequestDto;
import com.akbyk.cryptopal.trading.dto.TradeResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TradingService {

    private static final String FIAT_SYMBOL = "USD";

    private final WalletBalanceRepository walletBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;

    public TradingService(WalletBalanceRepository walletBalanceRepository,
                          TransactionRepository transactionRepository,
                          StringRedisTemplate redisTemplate) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * REPEATABLE_READ (rather than the READ_COMMITTED minimum) is used deliberately:
     * this method reads a wallet row, computes a new value from it, and writes it back
     * within the same transaction. READ_COMMITTED alone would still be safe here because
     * the pessimistic write lock below already serializes concurrent access to the same
     * row — but REPEATABLE_READ is kept as defense-in-depth so a second, unrelated read
     * of the same row later in this same transaction (e.g. if this method grows to also
     * re-check the row before logging) cannot observe a value changed by another
     * committed transaction mid-method.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TradeResponseDto executeTrade(UUID userId, TradeRequestDto request) {
        String symbol = request.getAssetSymbol().toUpperCase();
        BigDecimal volume = request.getVolume();

        if (volume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Trade volume must be greater than zero");
        }

        BigDecimal currentPrice = getCurrentPrice(symbol);

        return switch (request.getType()) {
            case BUY -> executeBuy(userId, symbol, volume, currentPrice);
            case SELL -> executeSell(userId, symbol, volume, currentPrice);
        };
    }

    private TradeResponseDto executeBuy(UUID userId, String symbol, BigDecimal volume, BigDecimal price) {
        // Lock order is fixed (USD first, then the asset) in BOTH executeBuy and
        // executeSell below, specifically to prevent a lock-ordering deadlock between
        // two concurrent trades on the same user's wallet rows.
        WalletBalanceEntity fiatWallet = lockOrCreateWallet(userId, FIAT_SYMBOL);
        WalletBalanceEntity assetWallet = lockOrCreateWallet(userId, symbol);

        BigDecimal cost = price.multiply(volume);
        if (fiatWallet.getAmount().compareTo(cost) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient USD balance: need " + cost + ", have " + fiatWallet.getAmount());
        }

        fiatWallet.setAmount(fiatWallet.getAmount().subtract(cost));
        assetWallet.setAmount(assetWallet.getAmount().add(volume));

        walletBalanceRepository.save(fiatWallet);
        walletBalanceRepository.save(assetWallet);

        TransactionEntity tx = logTransaction(userId, symbol, "BUY", volume, price);

        return new TradeResponseDto(tx.getId(), symbol, "BUY", volume, price,
                fiatWallet.getAmount(), assetWallet.getAmount());
    }

    private TradeResponseDto executeSell(UUID userId, String symbol, BigDecimal volume, BigDecimal price) {
        WalletBalanceEntity fiatWallet = lockOrCreateWallet(userId, FIAT_SYMBOL);
        WalletBalanceEntity assetWallet = lockOrCreateWallet(userId, symbol);

        if (assetWallet.getAmount().compareTo(volume) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient " + symbol + " balance: trying to sell " + volume
                            + ", have " + assetWallet.getAmount());
        }

        BigDecimal proceeds = price.multiply(volume);
        assetWallet.setAmount(assetWallet.getAmount().subtract(volume));
        fiatWallet.setAmount(fiatWallet.getAmount().add(proceeds));

        walletBalanceRepository.save(assetWallet);
        walletBalanceRepository.save(fiatWallet);

        TransactionEntity tx = logTransaction(userId, symbol, "SELL", volume, price);

        return new TradeResponseDto(tx.getId(), symbol, "SELL", volume, price,
                fiatWallet.getAmount(), assetWallet.getAmount());
    }

    private TransactionEntity logTransaction(UUID userId, String symbol, String type, BigDecimal volume, BigDecimal price) {
        TransactionEntity tx = new TransactionEntity();
        tx.setUserId(userId);
        tx.setAssetSymbol(symbol);
        tx.setTransactionType(type);
        tx.setVolume(volume);
        tx.setExecutionPrice(price);
        tx.setExecutedAt(OffsetDateTime.now());
        // If this insert throws, Spring's declarative @Transactional rolls back the
        // wallet updates saved just above in this same method. no partial state can ever be committed.
        return transactionRepository.save(tx);
    }

    private WalletBalanceEntity lockOrCreateWallet(UUID userId, String assetSymbol) {
        return walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, assetSymbol)
                .orElseGet(() -> {
                    WalletBalanceEntity newWallet = new WalletBalanceEntity();
                    newWallet.setUserId(userId);
                    newWallet.setAssetSymbol(assetSymbol);
                    newWallet.setAmount(BigDecimal.ZERO);
                    try {
                        return walletBalanceRepository.saveAndFlush(newWallet);
                    } catch (DataIntegrityViolationException ex) {
                        // Another concurrent transaction created this row first (unique
                        // constraint on user_id+asset_symbol) — lock and return that row
                        // instead of failing the trade outright.
                        return walletBalanceRepository.findByUserIdAndAssetSymbolForUpdate(userId, assetSymbol)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Wallet row for " + assetSymbol + " vanished unexpectedly"));
                    }
                });
    }

    private BigDecimal getCurrentPrice(String symbol) {
        String raw = redisTemplate.opsForValue().get("price:" + symbol);
        if (raw == null) {
            throw new AssetNotFoundException(
                    "No price available for " + symbol + " (untracked symbol, or cache not yet warmed)");
        }
        return new BigDecimal(raw);
    }
}