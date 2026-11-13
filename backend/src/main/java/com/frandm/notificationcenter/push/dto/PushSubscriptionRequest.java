package com.frandm.notificationcenter.push.dto;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionRequest(
        @NotBlank String endpoint,
        Keys keys,
        String p256dh,
        String auth
) {
    public record Keys(String p256dh, String auth) {}

    public String resolveP256dh() {
        if (p256dh != null && !p256dh.isBlank()) {
            return p256dh;
        }
        return keys != null ? keys.p256dh() : null;
    }

    public String resolveAuth() {
        if (auth != null && !auth.isBlank()) {
            return auth;
        }
        return keys != null ? keys.auth() : null;
    }
}
