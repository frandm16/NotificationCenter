package com.frandm.notificationcenter.token.service;

import com.frandm.notificationcenter.token.dto.CreateTokenRequest;
import com.frandm.notificationcenter.token.dto.CreatedTokenResponse;
import com.frandm.notificationcenter.token.dto.TokenResponse;
import com.frandm.notificationcenter.token.model.ApiToken;
import com.frandm.notificationcenter.token.repository.ApiTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiTokenService {

    private final ApiTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiTokenService(ApiTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public List<TokenResponse> getAllTokens() {
        return tokenRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TokenResponse::from)
                .toList();
    }

    @Transactional
    public CreatedTokenResponse createToken(CreateTokenRequest request) {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String randomHex = HexFormat.of().formatHex(randomBytes);
        String rawToken = "nc_live_" + randomHex;

        String prefix = rawToken.substring(0, 14);
        String tokenHash = hashToken(rawToken);
        String scopesJoined = String.join(",", request.scopes());

        ApiToken token = new ApiToken();
        token.setName(request.name().trim());
        token.setSource(request.source().trim().toLowerCase());
        token.setTokenPrefix(prefix);
        token.setTokenHash(tokenHash);
        token.setScopes(scopesJoined);
        token.setCreatedAt(OffsetDateTime.now());

        ApiToken saved = tokenRepository.save(token);
        return new CreatedTokenResponse(TokenResponse.from(saved), rawToken);
    }

    @Transactional
    public Optional<ApiToken> validateAndTouch(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hashToken(rawToken.trim());
        Optional<ApiToken> tokenOpt = tokenRepository.findByTokenHash(hash);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        ApiToken token = tokenOpt.get();
        if (!token.isActive()) {
            return Optional.empty();
        }
        token.setLastUsedAt(OffsetDateTime.now());
        return Optional.of(tokenRepository.save(token));
    }

    @Transactional
    public boolean revokeToken(Long id) {
        return tokenRepository.findById(id).map(token -> {
            token.setRevokedAt(OffsetDateTime.now());
            tokenRepository.save(token);
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean deleteToken(Long id) {
        return tokenRepository.findById(id).map(token -> {
            tokenRepository.delete(token);
            return true;
        }).orElse(false);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
