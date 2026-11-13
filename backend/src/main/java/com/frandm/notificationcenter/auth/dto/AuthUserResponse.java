package com.frandm.notificationcenter.auth.dto;

public record AuthUserResponse(
        boolean authenticated,
        String username
) {
}
