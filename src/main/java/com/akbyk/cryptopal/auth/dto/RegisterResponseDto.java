package com.akbyk.cryptopal.auth.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterResponseDto(UUID userId, String username, BigDecimal startingBalance) {
}