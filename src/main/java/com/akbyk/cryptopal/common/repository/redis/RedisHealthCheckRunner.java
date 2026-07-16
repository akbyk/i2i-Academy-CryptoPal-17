package com.akbyk.cryptopal.common.repository.redis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisHealthCheckRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthCheckRunner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            String key = "cryptopal:startup-healthcheck";
            redisTemplate.opsForValue().set(key, "ok");
            String result = redisTemplate.opsForValue().get(key);
            if ("ok".equals(result)) {
                log.info("Redis health-check passed: SET/GET round-trip successful.");
            } else {
                log.error("Redis health-check FAILED: expected 'ok', got '{}'", result);
            }
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis health-check CRITICAL: Could not connect to Redis", e);
        }
    }

    @PostConstruct
    public void init() {
        log.info("RedisHealthCheckRunner bean initialized.");
    }
}