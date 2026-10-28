package com.emilhuseynov.accesspass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emilhuseynov.accesspass.scan.ScanService;
import com.emilhuseynov.accesspass.support.ConcurrentScans;
import com.emilhuseynov.accesspass.support.TestFixtures;
import com.emilhuseynov.accesspass.support.TestFixtures.Scenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestFixtures.class)
@Testcontainers(disabledWithoutDocker = true)
class PostgresIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private ScanService scanService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void singleUsePassIsGrantedExactlyOnceUnderRealRowLocking() throws Exception {
        Scenario scenario = fixtures.scenario(1);

        ConcurrentScans.assertGranted(ConcurrentScans.run(scanService, scenario, 32), 1);
        assertThat(jdbc.queryForObject("select use_count from access_pass where id = ?", Integer.class,
                scenario.pass().pass().getId())).isEqualTo(1);
    }

    @Test
    void multiUsePassIsNeverOverConsumed() throws Exception {
        Scenario scenario = fixtures.scenario(5);

        ConcurrentScans.assertGranted(ConcurrentScans.run(scanService, scenario, 32), 5);
    }

    @Test
    void auditLogRejectsUpdatesDeletesAndTruncation() throws Exception {
        Scenario scenario = fixtures.scenario(1);
        ConcurrentScans.run(scanService, scenario, 1);

        assertThatThrownBy(() -> jdbc.update("update scan_audit set outcome = 'DENIED', reason = 'REVOKED'"))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from scan_audit"))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("truncate scan_audit"))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("append-only");
    }
}
