package com.akbyk.cryptopal.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.akbyk.cryptopal.common.repository.redis")
public class RedisRepositoryConfig {
}