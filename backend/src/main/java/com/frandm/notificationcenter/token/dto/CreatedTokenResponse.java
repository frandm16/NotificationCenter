package com.frandm.notificationcenter.token.dto;

public record CreatedTokenResponse(
        TokenResponse token,
        String rawToken
) {
}
