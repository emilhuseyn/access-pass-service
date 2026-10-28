package com.emilhuseynov.accesspass.scan;

import static org.assertj.core.api.Assertions.assertThat;

import com.emilhuseynov.accesspass.support.ConcurrentScans;
import com.emilhuseynov.accesspass.support.IntegrationTest;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class ConcurrentScanTest {

    private static final int SCANNERS = 16;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private ScanService scanService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void exactlyOneOfManySimultaneousScansOfASingleUsePassIsGranted() throws Exception {
        Scenario scenario = fixtures.scenario(1);

        List<ScanDecision> decisions = ConcurrentScans.run(scanService, scenario, SCANNERS);

        ConcurrentScans.assertGranted(decisions, 1);
        assertThat(useCount(scenario)).isEqualTo(1);
        assertThat(auditRows(scenario)).isEqualTo(SCANNERS);
    }

    @Test
    void aMultiUsePassIsNeverOverConsumedUnderContention() throws Exception {
        Scenario scenario = fixtures.scenario(3);

        List<ScanDecision> decisions = ConcurrentScans.run(scanService, scenario, SCANNERS);

        ConcurrentScans.assertGranted(decisions, 3);
        assertThat(useCount(scenario)).isEqualTo(3);
        assertThat(auditRows(scenario)).isEqualTo(SCANNERS);
    }

    private int useCount(Scenario scenario) {
        return jdbc.queryForObject("select use_count from access_pass where id = ?", Integer.class,
                scenario.pass().pass().getId());
    }

    private int auditRows(Scenario scenario) {
        return jdbc.queryForObject("select count(*) from scan_audit where pass_id = ?", Integer.class,
                scenario.pass().pass().getId());
    }
}
