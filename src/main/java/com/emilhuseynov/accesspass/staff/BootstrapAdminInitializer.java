package com.emilhuseynov.accesspass.staff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final BootstrapAdminProperties properties;
    private final StaffAccountRepository accounts;
    private final StaffService staffService;

    BootstrapAdminInitializer(BootstrapAdminProperties properties, StaffAccountRepository accounts,
                              StaffService staffService) {
        this.properties = properties;
        this.accounts = accounts;
        this.staffService = staffService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.username()) || !StringUtils.hasText(properties.password())) {
            return;
        }
        if (accounts.count() > 0) {
            return;
        }
        staffService.create(properties.username(), properties.password(), StaffRole.ADMIN);
        log.info("Created bootstrap admin account '{}'", properties.username());
    }

    @ConfigurationProperties("access-pass.bootstrap-admin")
    record BootstrapAdminProperties(String username, String password) {
    }
}
