package com.akbyk.cryptopal.auth;

import java.util.UUID;

public final class AuthenticatedUserContext {

    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();

    private AuthenticatedUserContext() {
    }

    public static void setCurrentUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}