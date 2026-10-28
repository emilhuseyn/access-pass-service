package com.emilhuseynov.accesspass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AccessPassApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessPassApplication.class, args);
    }
}
