package com.akbyk.cryptopal.trading;

/**
 * Thrown when there is no usable price for the requested asset symbol —
 * either the symbol isn't tracked at all, or the Redis price cache is cold
 * (no scheduler tick has run yet since app startup).
 */
public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException(String message) {
        super(message);
    }
}