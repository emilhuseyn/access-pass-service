package com.emilhuseynov.accesspass.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.emilhuseynov.accesspass.device.DevicePrincipal;
import com.emilhuseynov.accesspass.scan.DenyReason;
import com.emilhuseynov.accesspass.scan.ScanDecision;
import com.emilhuseynov.accesspass.scan.ScanService;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ConcurrentScans {

    private ConcurrentScans() {
    }

    public static List<ScanDecision> run(ScanService scanService, Scenario scenario, int scanners) throws Exception {
        DevicePrincipal device = new DevicePrincipal(scenario.device().device().getId(), scenario.gate().getId(),
                scenario.device().device().getName());
        CountDownLatch ready = new CountDownLatch(scanners);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(scanners)) {
            List<Future<ScanDecision>> futures = new ArrayList<>();
            for (int i = 0; i < scanners; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return scanService.scan(device, scenario.token());
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ScanDecision> decisions = new ArrayList<>();
            for (Future<ScanDecision> future : futures) {
                decisions.add(future.get(30, TimeUnit.SECONDS));
            }
            return decisions;
        }
    }

    public static void assertGranted(List<ScanDecision> decisions, int expectedGrants) {
        assertThat(decisions).filteredOn(ScanDecision::isGranted).hasSize(expectedGrants);
        assertThat(decisions).filteredOn(decision -> !decision.isGranted())
                .extracting(ScanDecision::reason)
                .allMatch(reason -> reason == DenyReason.USES_EXHAUSTED || reason == DenyReason.CONCURRENT_SCAN);
    }
}
