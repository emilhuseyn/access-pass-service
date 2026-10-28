package com.emilhuseynov.accesspass.security;

import com.emilhuseynov.accesspass.device.DeviceApiKeyAuthenticationConverter;
import com.emilhuseynov.accesspass.device.DeviceApiKeyAuthenticationProvider;
import com.emilhuseynov.accesspass.device.DeviceAuthenticationToken;
import com.emilhuseynov.accesspass.device.GateDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFilter;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String OPERATOR = "OPERATOR";

    @Bean
    @Order(1)
    SecurityFilterChain deviceApiChain(HttpSecurity http, GateDeviceRepository devices, ObjectMapper objectMapper)
            throws Exception {
        ProblemDetailSecurityHandler problemHandler = new ProblemDetailSecurityHandler(objectMapper, "DeviceKey");
        AuthenticationFilter deviceKeyFilter = new AuthenticationFilter(
                new ProviderManager(new DeviceApiKeyAuthenticationProvider(devices)),
                new DeviceApiKeyAuthenticationConverter());
        deviceKeyFilter.setSuccessHandler((request, response, authentication) -> {
        });
        deviceKeyFilter.setFailureHandler(new AuthenticationEntryPointFailureHandler(problemHandler));

        return statelessApi(http)
                .securityMatcher("/api/v1/scans/**")
                .addFilterBefore(deviceKeyFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(DeviceAuthenticationToken.ROLE))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain staffApiChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        ProblemDetailSecurityHandler problemHandler = new ProblemDetailSecurityHandler(objectMapper, "Bearer");
        return statelessApi(http)
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                        .requestMatchers("/api/v1/staff/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ADMIN, OPERATOR)
                        .requestMatchers("/api/v1/holders/**", "/api/v1/passes/**").hasAnyRole(ADMIN, OPERATOR)
                        .anyRequest().hasRole(ADMIN))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemHandler).accessDeniedHandler(problemHandler))
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        return statelessApi(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    private static HttpSecurity statelessApi(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtTokenIssuer.ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static SecretKey jwtKey(JwtProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.secret());
        if (secret.length < 32) {
            throw new IllegalStateException("access-pass.jwt.secret must decode to at least 32 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
