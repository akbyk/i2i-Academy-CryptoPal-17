package com.akbyk.cryptopal.auth.dto;

public record AuthResponseDto(String token, long expiresInSeconds) {
}