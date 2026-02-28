package com.univerliga.identityprovisioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class UniverligaIdentityProvisioningApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniverligaIdentityProvisioningApplication.class, args);
    }
}
