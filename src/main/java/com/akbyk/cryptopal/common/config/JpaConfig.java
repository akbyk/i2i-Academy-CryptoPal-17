package com.akbyk.cryptopal.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.akbyk.cryptopal.common.repository.jpa")
public class JpaConfig {
}