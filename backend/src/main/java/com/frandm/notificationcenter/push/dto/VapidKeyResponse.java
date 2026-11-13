package com.frandm.notificationcenter.push.dto;

public record VapidKeyResponse(
        String publicKey,
        boolean enabled
) {
}
