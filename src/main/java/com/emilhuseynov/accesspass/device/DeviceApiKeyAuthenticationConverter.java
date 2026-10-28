package com.emilhuseynov.accesspass.device;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

public final class DeviceApiKeyAuthenticationConverter implements AuthenticationConverter {

    public static final String HEADER = "X-Device-Key";

    @Override
    public Authentication convert(HttpServletRequest request) {
        String apiKey = request.getHeader(HEADER);
        return StringUtils.hasText(apiKey) ? DeviceAuthenticationToken.unauthenticated(apiKey.strip()) : null;
    }
}
