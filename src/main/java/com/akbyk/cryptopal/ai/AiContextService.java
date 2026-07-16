package com.akbyk.cryptopal.ai;

import com.akbyk.cryptopal.common.entity.PriceTrendLogEntity;
import com.akbyk.cryptopal.common.entity.TransactionEntity;
import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import com.akbyk.cryptopal.common.repository.jpa.PriceTrendLogRepository;
import com.akbyk.cryptopal.common.repository.jpa.TransactionRepository;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import com.akbyk.cryptopal.market.MarketProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AiContextService {

    private final WalletBalanceRepository walletBalanceRepository;
    private final TransactionRepository transactionRepository;
    private final PriceTrendLogRepository priceTrendLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final MarketProperties marketProperties;

    @Value("${cryptopal.ai.context.transaction-limit:15}")
    private int transactionLimit;

    @Value("${cryptopal.ai.context.price-trend-points:24}")
    private int priceTrendPoints;

    public AiContextService(WalletBalanceRepository walletBalanceRepository,
                            TransactionRepository transactionRepository,
                            PriceTrendLogRepository priceTrendLogRepository,
                            StringRedisTemplate redisTemplate,
                            MarketProperties marketProperties) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.priceTrendLogRepository = priceTrendLogRepository;
        this.redisTemplate = redisTemplate;
        this.marketProperties = marketProperties;
    }

    public String buildContextPrompt(UUID userId, String userMessage) {
        List<WalletBalanceEntity> balances = walletBalanceRepository.findAllByUserId(userId);

        List<TransactionEntity> recentTransactions = transactionRepository
                .findByUserIdOrderByExecutedAtDesc(userId, PageRequest.of(0, transactionLimit));

        // Latest price per asset: read from Redis
        Map<String, BigDecimal> latestPrices = new LinkedHashMap<>();
        for (String symbol : marketProperties.getTrackedSymbols()) {
            String raw = redisTemplate.opsForValue().get("price:" + symbol);
            if (raw != null) {
                latestPrices.put(symbol, new BigDecimal(raw));
            }
        }

        // Historical price trend: read from Postgres price_trend_log, most recent first.
        Map<String, List<PriceTrendLogEntity>> priceTrends = new LinkedHashMap<>();
        for (String symbol : marketProperties.getTrackedSymbols()) {
            List<PriceTrendLogEntity> points = priceTrendLogRepository
                    .findRecentBySymbol(symbol, PageRequest.of(0, priceTrendPoints));
            priceTrends.put(symbol, points);
        }

        return assemblePrompt(balances, recentTransactions, priceTrends, latestPrices, userMessage);
    }

    private String assemblePrompt(List<WalletBalanceEntity> balances,
                                  List<TransactionEntity> recentTransactions,
                                  Map<String, List<PriceTrendLogEntity>> priceTrends,
                                  Map<String, BigDecimal> latestPrices,
                                  String userMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append("SYSTEM: You are a portfolio assistant for a SIMULATED crypto trading app. ")
                .append("Use ONLY the data sections below to answer the user's question. ")
                .append("This is not real money and not real financial advice.\n\n");

        sb.append("USER PORTFOLIO:\n");
        if (balances.isEmpty()) {
            sb.append("(no wallet rows found)\n");
        }
        for (WalletBalanceEntity b : balances) {
            sb.append("- ").append(b.getAssetSymbol()).append(": ").append(b.getAmount()).append("\n");
        }

        sb.append("\nRECENT TRANSACTIONS:\n");
        if (recentTransactions.isEmpty()) {
            sb.append("(no transactions yet)\n");
        }
        for (TransactionEntity tx : recentTransactions) {
            sb.append("- ").append(tx.getExecutedAt()).append(" ")
                    .append(tx.getTransactionType()).append(" ")
                    .append(tx.getVolume()).append(" ").append(tx.getAssetSymbol())
                    .append(" @ ").append(tx.getExecutionPrice()).append("\n");
        }

        sb.append("\nPRICE TRENDS (most recent first, per asset):\n");
        priceTrends.forEach((symbol, points) -> {
            String series = points.stream()
                    .map(p -> p.getPrice().toPlainString())
                    .collect(Collectors.joining(", "));
            sb.append("- ").append(symbol).append(": ").append(series.isEmpty() ? "(no data yet)" : series).append("\n");
        });

        sb.append("\nLATEST PRICES (real-time, from cache):\n");
        latestPrices.forEach((symbol, price) ->
                sb.append("- ").append(symbol).append(": ").append(price).append("\n"));

        sb.append("\nUSER QUERY:\n").append(userMessage).append("\n");

        return sb.toString();
    }
}