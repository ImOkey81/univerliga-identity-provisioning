package com.univerliga.identityprovisioning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String apiVersion = "v1";
    private Provisioning provisioning = new Provisioning();
    private Keycloak keycloak = new Keycloak();

    @Getter
    @Setter
    public static class Provisioning {
        private String mode = "mock-crm";
        private Broker broker = new Broker();
    }

    @Getter
    @Setter
    public static class Broker {
        private String exchange;
        private String queue;
        private String dlq;
        private String routingCreated;
        private String routingUpdated;
        private String routingDeactivated;
        private int maxRetries = 5;
    }

    @Getter
    @Setter
    public static class Keycloak {
        private String baseUrl;
        private String realm;
        private String clientId;
        private String clientSecret;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
    }
}
