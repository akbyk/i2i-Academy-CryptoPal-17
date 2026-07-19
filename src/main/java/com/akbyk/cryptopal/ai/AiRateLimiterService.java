package com.akbyk.cryptopal.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class AiRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${cryptopal.ai.rate-limit.max-requests:8}")
    private int maxRequests;

    @Value("${cryptopal.ai.rate-limit.window-seconds:60}")
    private int windowSeconds;

    public AiRateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAndRecord(UUID userId) {
        String key = "ai:ratelimit:" + userId;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (count != null && count > maxRequests) {
            throw new AiRateLimitExceededException(
                    "Too many AI requests. Please wait a bit before asking again.");
        }
    }
}