package com.frandm.notificationcenter.token.dto;

import com.frandm.notificationcenter.token.model.ApiToken;
import java.time.OffsetDateTime;
import java.util.Set;

public record TokenResponse(
        Long id,
        String name,
        String tokenPrefix,
        String source,
        Set<String> scopes,
        OffsetDateTime createdAt,
        OffsetDateTime lastUsedAt,
        OffsetDateTime revokedAt,
        boolean active
) {
    public static TokenResponse from(ApiToken token) {
        return new TokenResponse(
                token.getId(),
                token.getName(),
                token.getTokenPrefix(),
                token.getSource(),
                token.getScopeSet(),
                token.getCreatedAt(),
                token.getLastUsedAt(),
                token.getRevokedAt(),
                token.isActive()
        );
    }
}
