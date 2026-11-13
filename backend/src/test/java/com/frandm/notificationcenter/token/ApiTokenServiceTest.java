package com.frandm.notificationcenter.token;

import com.frandm.notificationcenter.token.dto.CreateTokenRequest;
import com.frandm.notificationcenter.token.dto.CreatedTokenResponse;
import com.frandm.notificationcenter.token.model.ApiToken;
import com.frandm.notificationcenter.token.repository.ApiTokenRepository;
import com.frandm.notificationcenter.token.service.ApiTokenService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ApiTokenServiceTest {

    @Autowired
    private ApiTokenRepository tokenRepository;

    private ApiTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new ApiTokenService(tokenRepository);
    }

    @Test
    void shouldCreateTokenWithHashAndPrefix() {
        CreateTokenRequest request = new CreateTokenRequest("StudyZen Alert Service", "studyzen", Set.of("notify", "read"));
        CreatedTokenResponse response = tokenService.createToken(request);

        assertThat(response.rawToken()).startsWith("nc_live_");
        assertThat(response.token().name()).isEqualTo("StudyZen Alert Service");
        assertThat(response.token().source()).isEqualTo("studyzen");
        assertThat(response.token().tokenPrefix()).isEqualTo(response.rawToken().substring(0, 14));
        assertThat(response.token().scopes()).containsExactlyInAnyOrder("notify", "read");
        assertThat(response.token().active()).isTrue();

        Optional<ApiToken> validated = tokenService.validateAndTouch(response.rawToken());
        assertThat(validated).isPresent();
        assertThat(validated.get().getSource()).isEqualTo("studyzen");
        assertThat(validated.get().getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldRevokeToken() {
        CreateTokenRequest request = new CreateTokenRequest("Proxmox Node", "proxmox", Set.of("notify"));
        CreatedTokenResponse response = tokenService.createToken(request);

        boolean revoked = tokenService.revokeToken(response.token().id());
        assertThat(revoked).isTrue();

        Optional<ApiToken> validated = tokenService.validateAndTouch(response.rawToken());
        assertThat(validated).isEmpty();
    }
}
