package com.frandm.notificationcenter.token.controller;

import com.frandm.notificationcenter.token.dto.CreateTokenRequest;
import com.frandm.notificationcenter.token.dto.CreatedTokenResponse;
import com.frandm.notificationcenter.token.dto.TokenResponse;
import com.frandm.notificationcenter.token.service.ApiTokenService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokens")
@PreAuthorize("hasRole('ADMIN')")
public class ApiTokenController {

    private final ApiTokenService tokenService;

    public ApiTokenController(ApiTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<TokenResponse> listTokens() {
        return tokenService.getAllTokens();
    }

    @PostMapping
    public ResponseEntity<CreatedTokenResponse> createToken(@Valid @RequestBody CreateTokenRequest request) {
        CreatedTokenResponse response = tokenService.createToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeToken(@PathVariable Long id) {
        boolean revoked = tokenService.revokeToken(id);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable Long id) {
        boolean deleted = tokenService.deleteToken(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
