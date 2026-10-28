package com.emilhuseynov.accesspass.device;

import com.emilhuseynov.accesspass.device.DeviceService.RegisteredDevice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/api/v1/gates/{gateId}/devices")
    ResponseEntity<DeviceCredentialsResponse> register(@PathVariable Long gateId,
                                                       @Valid @RequestBody RegisterDeviceRequest request) {
        return credentials(HttpStatus.CREATED, deviceService.register(gateId, request.name()));
    }

    @GetMapping("/api/v1/gates/{gateId}/devices")
    List<DeviceResponse> listForGate(@PathVariable Long gateId) {
        return deviceService.listForGate(gateId).stream().map(DeviceResponse::from).toList();
    }

    @PostMapping("/api/v1/devices/{deviceId}/rotate-key")
    ResponseEntity<DeviceCredentialsResponse> rotateKey(@PathVariable Long deviceId) {
        return credentials(HttpStatus.OK, deviceService.rotateKey(deviceId));
    }

    @PostMapping("/api/v1/devices/{deviceId}/deactivate")
    DeviceResponse deactivate(@PathVariable Long deviceId) {
        return DeviceResponse.from(deviceService.deactivate(deviceId));
    }

    private static ResponseEntity<DeviceCredentialsResponse> credentials(HttpStatus status, RegisteredDevice registered) {
        GateDevice device = registered.device();
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(new DeviceCredentialsResponse(device.getId(), device.getGate().getId(), device.getName(),
                        registered.apiKey()));
    }

    record RegisterDeviceRequest(@NotBlank @Size(max = 120) String name) {
    }

    record DeviceCredentialsResponse(Long deviceId, Long gateId, String name, String apiKey) {
    }

    record DeviceResponse(Long id, Long gateId, String name, boolean active, Instant createdAt,
                          Instant deactivatedAt) {

        static DeviceResponse from(GateDevice device) {
            return new DeviceResponse(device.getId(), device.getGate().getId(), device.getName(), device.isActive(),
                    device.getCreatedAt(), device.getDeactivatedAt());
        }
    }
}
