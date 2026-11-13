package com.frandm.notificationcenter.security.model;

import com.frandm.notificationcenter.token.model.ApiToken;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiTokenAuthenticationToken extends AbstractAuthenticationToken {

    private final ApiToken apiToken;

    public ApiTokenAuthenticationToken(ApiToken apiToken, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiToken = apiToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return apiToken.getSource();
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public String getSource() {
        return apiToken.getSource();
    }
}
