package com.emilhuseynov.accesspass.scan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emilhuseynov.accesspass.staff.StaffRole;
import com.emilhuseynov.accesspass.support.IntegrationTest;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ScanAuditApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestFixtures fixtures;

    private Scenario scenario;

    @BeforeEach
    void scanThreeTimes() throws Exception {
        scenario = fixtures.scenario(1);
        scan(scenario.token());
        scan(scenario.token());
        scan("AP1.garbage");
    }

    @Test
    void filtersByGateAndReturnsNewestFirst() throws Exception {
        mvc.perform(get("/api/v1/audit/scans")
                        .param("gateId", scenario.gate().getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.OPERATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].reason").value("MALFORMED_TOKEN"))
                .andExpect(jsonPath("$.items[0].passId").doesNotExist())
                .andExpect(jsonPath("$.items[2].outcome").value("GRANTED"));
    }

    @Test
    void filtersByPassOutcomeAndReason() throws Exception {
        mvc.perform(get("/api/v1/audit/scans")
                        .param("passId", scenario.pass().pass().getId().toString())
                        .param("outcome", "DENIED")
                        .param("reason", "USES_EXHAUSTED")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].holderId").value(scenario.holder().getId()))
                .andExpect(jsonPath("$.items[0].tokenFingerprint").isString());
    }

    @Test
    void pagesThroughResults() throws Exception {
        mvc.perform(get("/api/v1/audit/scans")
                        .param("deviceId", scenario.device().device().getId().toString())
                        .param("page", "1")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.OPERATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void rejectsInvertedTimeRangeAndOversizedPages() throws Exception {
        String operator = fixtures.bearer(StaffRole.OPERATOR);

        mvc.perform(get("/api/v1/audit/scans")
                        .param("from", "2026-10-28T12:00:00Z")
                        .param("to", "2026-10-28T11:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, operator))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/audit/scans")
                        .param("size", "1000")
                        .header(HttpHeaders.AUTHORIZATION, operator))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deviceCannotReadTheAuditLog() throws Exception {
        mvc.perform(get("/api/v1/audit/scans").header("X-Device-Key", scenario.deviceKey()))
                .andExpect(status().isUnauthorized());
    }

    private void scan(String token) throws Exception {
        mvc.perform(post("/api/v1/scans")
                        .header("X-Device-Key", scenario.deviceKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}
                                """.formatted(token)))
                .andExpect(status().isOk());
    }
}
