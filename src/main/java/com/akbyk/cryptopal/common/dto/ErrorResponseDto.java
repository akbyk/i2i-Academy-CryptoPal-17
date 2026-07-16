package com.akbyk.cryptopal.common.dto;

import java.time.Instant;

public record ErrorResponseDto(String errorCode, String message, Instant timestamp) {
    public ErrorResponseDto(String errorCode, String message) {
        this(errorCode, message, Instant.now());
    }
}