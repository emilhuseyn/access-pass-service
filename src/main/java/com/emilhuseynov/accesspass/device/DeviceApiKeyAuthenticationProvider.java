package com.emilhuseynov.accesspass.device;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * Device keys are 256-bit random values, so a single SHA-256 is enough to make a leaked database useless and lets
 * the lookup go straight to a unique index instead of iterating over slow password hashes.
 */
public final class DeviceApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final GateDeviceRepository devices;

    public DeviceApiKeyAuthenticationProvider(GateDeviceRepository devices) {
        this.devices = devices;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String apiKey = (String) authentication.getCredentials();
        return devices.findActivePrincipalByApiKeyHash(ApiKeys.hash(apiKey))
                .map(DeviceAuthenticationToken::authenticated)
                .orElseThrow(() -> new BadCredentialsException("Unknown or inactive device key"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return DeviceAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
