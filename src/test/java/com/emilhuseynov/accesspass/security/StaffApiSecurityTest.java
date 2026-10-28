package com.emilhuseynov.accesspass.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emilhuseynov.accesspass.device.DeviceService.RegisteredDevice;
import com.emilhuseynov.accesspass.staff.StaffRole;
import com.emilhuseynov.accesspass.staff.StaffService;
import com.emilhuseynov.accesspass.support.IntegrationTest;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class StaffApiSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private StaffService staffService;

    @Test
    void anonymousRequestGetsProblemDetailAndBearerChallenge() throws Exception {
        mvc.perform(get("/api/v1/sites"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void operatorCanReadButNotChangeSiteConfiguration() throws Exception {
        String operator = fixtures.bearer(StaffRole.OPERATOR);

        mvc.perform(get("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, operator))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Operator site", "timeZone": "Asia/Baku"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void adminCanChangeSiteConfiguration() throws Exception {
        mvc.perform(post("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Admin site %s", "timeZone": "Asia/Baku"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeZone").value("Asia/Baku"));
    }

    @Test
    void operatorCannotManageStaffAccounts() throws Exception {
        mvc.perform(get("/api/v1/staff").header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.OPERATOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deviceKeyIsNotAcceptedOnStaffEndpoints() throws Exception {
        RegisteredDevice device = fixtures.device(fixtures.gate(fixtures.site()));

        mvc.perform(get("/api/v1/sites").header("X-Device-Key", device.apiKey()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + device.apiKey()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtSignedWithAnotherSecretIsRejected() throws Exception {
        byte[] otherSecret = "an-attacker-chosen-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        NimbusJwtEncoder forger = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("access-pass-service")
                .subject("mallory")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("roles", List.of("ADMIN"))
                .build();
        String forged = forger.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffCanExchangeCredentialsForAWorkingToken() throws Exception {
        String username = "op-" + UUID.randomUUID().toString().substring(0, 8);
        staffService.create(username, "correct horse battery staple", StaffRole.OPERATOR);

        String body = mvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "correct horse battery staple"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");

        mvc.perform(get("/api/v1/sites").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordUnknownUserAndDisabledAccountAllGetTheSameAnswer() throws Exception {
        String username = "op-" + UUID.randomUUID().toString().substring(0, 8);
        String disabled = "op-" + UUID.randomUUID().toString().substring(0, 8);
        staffService.create(username, "correct horse battery staple", StaffRole.OPERATOR);
        staffService.disable(staffService.create(disabled, "correct horse battery staple", StaffRole.OPERATOR).getId());

        for (String[] attempt : List.of(
                new String[] {username, "wrong password"},
                new String[] {"nobody-" + UUID.randomUUID(), "correct horse battery staple"},
                new String[] {disabled, "correct horse battery staple"})) {
            mvc.perform(post("/api/v1/auth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "%s", "password": "%s"}
                                    """.formatted(attempt[0], attempt[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid username or password"));
        }
    }

    @Test
    void apiDocumentationIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void unknownPathsOutsideTheApiAreDenied() throws Exception {
        mvc.perform(get("/internal/anything")).andExpect(status().isForbidden());
    }
}
