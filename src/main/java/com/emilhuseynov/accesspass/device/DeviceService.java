package com.emilhuseynov.accesspass.device;

import com.emilhuseynov.accesspass.common.ConflictException;
import com.emilhuseynov.accesspass.common.NotFoundException;
import com.emilhuseynov.accesspass.site.Gate;
import com.emilhuseynov.accesspass.site.SiteService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final GateDeviceRepository devices;
    private final SiteService siteService;
    private final Clock clock;

    public DeviceService(GateDeviceRepository devices, SiteService siteService, Clock clock) {
        this.devices = devices;
        this.siteService = siteService;
        this.clock = clock;
    }

    @Transactional
    public RegisteredDevice register(Long gateId, String name) {
        Gate gate = siteService.getGate(gateId);
        String apiKey = ApiKeys.generate();
        GateDevice device = devices.save(new GateDevice(gate, name, ApiKeys.hash(apiKey), clock.instant()));
        return new RegisteredDevice(device, apiKey);
    }

    @Transactional
    public RegisteredDevice rotateKey(Long deviceId) {
        GateDevice device = get(deviceId);
        if (!device.isActive()) {
            throw new ConflictException("Device " + deviceId + " is deactivated");
        }
        String apiKey = ApiKeys.generate();
        device.replaceApiKeyHash(ApiKeys.hash(apiKey));
        return new RegisteredDevice(device, apiKey);
    }

    @Transactional
    public GateDevice deactivate(Long deviceId) {
        GateDevice device = get(deviceId);
        if (device.isActive()) {
            device.deactivate(clock.instant());
        }
        return device;
    }

    @Transactional(readOnly = true)
    public List<GateDevice> listForGate(Long gateId) {
        siteService.getGate(gateId);
        return devices.findByGateIdOrderByName(gateId);
    }

    private GateDevice get(Long deviceId) {
        return devices.findById(deviceId).orElseThrow(() -> new NotFoundException("Device", deviceId));
    }

    public record RegisteredDevice(GateDevice device, String apiKey) {
    }
}
