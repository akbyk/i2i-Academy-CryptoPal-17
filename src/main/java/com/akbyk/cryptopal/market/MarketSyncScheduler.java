package com.akbyk.cryptopal.market;


import com.akbyk.cryptopal.common.entity.PriceTrendLogEntity;
import com.akbyk.cryptopal.common.repository.jpa.PriceTrendLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MarketSyncScheduler {

    private final PriceProvider priceProvider;
    private final StringRedisTemplate redisTemplate;
    private final PriceTrendLogRepository priceTrendLogRepository;

    public MarketSyncScheduler(PriceProvider priceProvider,
                               StringRedisTemplate redisTemplate,
                               PriceTrendLogRepository priceTrendLogRepository) {
        this.priceProvider = priceProvider;
        this.redisTemplate = redisTemplate;
        this.priceTrendLogRepository = priceTrendLogRepository;
    }

    @Scheduled(fixedRate = 15000)
    public void syncPrices() {
        log.debug("Market sync tick running on thread: {}", Thread.currentThread().getName());

        Map<String, BigDecimal> latestPrices = priceProvider.fetchLatestPrices();
        if (latestPrices.isEmpty()) {
            log.warn("PriceProvider returned no prices this tick; skipping sync entirely.");
            return;
        }

        // Step 1: Redis cache overwrite. No TTL — only the latest value is retained,
        // overwritten in place each tick, per spec requirement 5.4.1.
        latestPrices.forEach((symbol, price) ->
                redisTemplate.opsForValue().set("price:" + symbol, price.toPlainString()));

        // Step 2: Historical logging, isolated in its own try-catch so a transient
        // Postgres failure never prevents the Redis cache above from being fresh —
        // cache freshness must not be coupled to historical-log durability.
        try {
            OffsetDateTime now = OffsetDateTime.now();
            List<PriceTrendLogEntity> rows = latestPrices.entrySet().stream()
                    .map(entry -> {
                        PriceTrendLogEntity row = new PriceTrendLogEntity();
                        row.setAssetSymbol(entry.getKey());
                        row.setPrice(entry.getValue());
                        row.setRecordedAt(now);
                        return row;
                    })
                    .toList();
            priceTrendLogRepository.saveAll(rows);
        } catch (Exception ex) {
            log.error("Failed to persist price_trend_log rows this tick; Redis cache was still updated.", ex);
        }
    }
}