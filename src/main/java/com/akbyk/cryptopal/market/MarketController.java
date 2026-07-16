package com.akbyk.cryptopal.market;

import com.akbyk.cryptopal.common.dto.ErrorResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/market")
@Tag(name = "Market Data")
public class MarketController {

    private final StringRedisTemplate redisTemplate;
    private final MarketProperties marketProperties;

    public MarketController(StringRedisTemplate redisTemplate, MarketProperties marketProperties) {
        this.redisTemplate = redisTemplate;
        this.marketProperties = marketProperties;
    }

    @GetMapping("/prices")
    @Operation(summary = "Get the latest Redis-cached price for every tracked asset (zero DB access)")
    public ResponseEntity<?> getPrices() {
        List<String> symbols = marketProperties.getTrackedSymbols();
        List<String> keys = symbols.stream().map(s -> "price:" + s).toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        if (values == null || values.stream().allMatch(Objects::isNull)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponseDto("CACHE_WARMING",
                            "Price cache is still warming up (no sync tick has completed yet); try again shortly."));
        }

        Instant now = Instant.now();
        List<PriceDto> result = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) {
            String value = values.get(i);
            if (value == null) {
                log.warn("No cached price yet for symbol {}", symbols.get(i));
                continue;
            }
            result.add(new PriceDto(symbols.get(i), new BigDecimal(value), now));
        }
        return ResponseEntity.ok(result);
    }
}