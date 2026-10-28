package com.emilhuseynov.accesspass.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emilhuseynov.accesspass.device.DeviceService;
import com.emilhuseynov.accesspass.device.DeviceService.RegisteredDevice;
import com.emilhuseynov.accesspass.staff.StaffRole;
import com.emilhuseynov.accesspass.support.IntegrationTest;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class ScanApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private DeviceService deviceService;

    @Test
    void singleUsePassIsGrantedOnceThenDenied() throws Exception {
        Scenario scenario = fixtures.scenario(1);

        scan(scenario.deviceKey(), scenario.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("GRANTED"))
                .andExpect(jsonPath("$.holderName").value(scenario.holder().getFullName()))
                .andExpect(jsonPath("$.reason").doesNotExist());
        scan(scenario.deviceKey(), scenario.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DENIED"))
                .andExpect(jsonPath("$.reason").value("USES_EXHAUSTED"))
                .andExpect(jsonPath("$.holderName").doesNotExist());
    }

    @Test
    void gateIsTakenFromTheAuthenticatedDeviceNotFromTheRequest() throws Exception {
        Scenario scenario = fixtures.scenario(null);
        RegisteredDevice elsewhere = fixtures.device(fixtures.gate(scenario.site()));

        scan(elsewhere.apiKey(), scenario.token())
                .andExpect(jsonPath("$.decision").value("DENIED"))
                .andExpect(jsonPath("$.reason").value("GATE_NOT_ALLOWED"));
    }

    @Test
    void revokedPassIsDenied() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        mvc.perform(post("/api/v1/passes/{id}/revoke", scenario.pass().pass().getId())
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Badge reported lost"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        scan(scenario.deviceKey(), scenario.token())
                .andExpect(jsonPath("$.decision").value("DENIED"))
                .andExpect(jsonPath("$.reason").value("REVOKED"));
    }

    @Test
    void staffJwtCannotBeUsedToScan() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        mvc.perform(post("/api/v1/scans")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(scenario.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "DeviceKey"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void scanWithoutOrWithWrongDeviceKeyIsUnauthorized() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        mvc.perform(post("/api/v1/scans").contentType(MediaType.APPLICATION_JSON).content(body(scenario.token())))
                .andExpect(status().isUnauthorized());
        scan("gdk_" + "A".repeat(43), scenario.token())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void deactivatedDeviceAndRotatedKeyStopWorkingImmediately() throws Exception {
        Scenario scenario = fixtures.scenario(null);
        String originalKey = scenario.deviceKey();
        String rotatedKey = deviceService.rotateKey(scenario.device().device().getId()).apiKey();

        scan(originalKey, scenario.token()).andExpect(status().isUnauthorized());
        scan(rotatedKey, scenario.token()).andExpect(status().isOk());

        deviceService.deactivate(scenario.device().device().getId());

        scan(rotatedKey, scenario.token()).andExpect(status().isUnauthorized());
    }

    @Test
    void qrCodeEncodesTheSameTokenThatWasIssued() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        byte[] png = mvc.perform(get("/api/v1/passes/{id}/qr", scenario.pass().pass().getId())
                        .header(HttpHeaders.AUTHORIZATION, fixtures.bearer(StaffRole.OPERATOR)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn().getResponse().getContentAsByteArray();

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))));
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(scenario.token());
    }

    @Test
    void deviceKeyCannotFetchQrCodes() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        mvc.perform(get("/api/v1/passes/{id}/qr", scenario.pass().pass().getId())
                        .header("X-Device-Key", scenario.deviceKey()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oversizedTokenIsRejectedAsBadRequest() throws Exception {
        Scenario scenario = fixtures.scenario(null);

        scan(scenario.deviceKey(), "A".repeat(600))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());
    }

    private ResultActions scan(String deviceKey, String token) throws Exception {
        return mvc.perform(post("/api/v1/scans")
                .header("X-Device-Key", deviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(token)));
    }

    private static String body(String token) {
        return """
                {"token": "%s"}
                """.formatted(token);
    }
}
