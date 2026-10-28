package com.emilhuseynov.accesspass.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer {

    static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedJwt issue(String subject, Collection<String> roles) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .claim(ROLES_CLAIM, List.copyOf(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedJwt(value, properties.ttl());
    }

    public record IssuedJwt(String value, Duration expiresIn) {
    }
}
