package com.emilhuseynov.accesspass.device;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public final class DeviceAuthenticationToken extends AbstractAuthenticationToken {

    public static final String ROLE = "DEVICE";

    private final DevicePrincipal principal;
    private String apiKey;

    private DeviceAuthenticationToken(String apiKey) {
        super(List.of());
        this.principal = null;
        this.apiKey = apiKey;
    }

    private DeviceAuthenticationToken(DevicePrincipal principal) {
        super(AuthorityUtils.createAuthorityList("ROLE_" + ROLE));
        this.principal = principal;
        super.setAuthenticated(true);
    }

    static DeviceAuthenticationToken unauthenticated(String apiKey) {
        return new DeviceAuthenticationToken(apiKey);
    }

    static DeviceAuthenticationToken authenticated(DevicePrincipal principal) {
        return new DeviceAuthenticationToken(principal);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public DevicePrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal == null ? "unauthenticated-device" : "device:" + principal.deviceId();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Use DeviceAuthenticationToken.authenticated(...) instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.apiKey = null;
    }

    @Override
    public String toString() {
        return "DeviceAuthenticationToken[" + getName() + ", authenticated=" + isAuthenticated() + "]";
    }
}
