package com.akbyk.cryptopal.ai;

public class AiRateLimitExceededException extends RuntimeException {
    public AiRateLimitExceededException(String message) {
        super(message);
    }
}