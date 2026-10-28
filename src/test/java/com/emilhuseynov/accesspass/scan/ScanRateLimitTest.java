package com.emilhuseynov.accesspass.scan;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emilhuseynov.accesspass.ratelimit.RateLimiter;
import com.emilhuseynov.accesspass.support.IntegrationTest;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ScanRateLimitTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestFixtures fixtures;

    @MockBean
    private RateLimiter rateLimiter;

    @SpyBean
    private ScanService scanService;

    @Test
    void throttledDeviceGets429AndTheScanIsNotEvaluated() throws Exception {
        Scenario scenario = fixtures.scenario(1);
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        mvc.perform(post("/api/v1/scans")
                        .header("X-Device-Key", scenario.deviceKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}
                                """.formatted(scenario.token())))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
                .andExpect(jsonPath("$.title").value("Rate limit exceeded"));
        verifyNoInteractions(scanService);
    }
}
