package com.univerliga.identityprovisioning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient keycloakRestClient(AppProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(props.getKeycloak().getConnectTimeoutMs()))
            .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(props.getKeycloak().getReadTimeoutMs()));

        return RestClient.builder()
            .requestFactory(factory)
            .baseUrl(props.getKeycloak().getBaseUrl())
            .build();
    }
}
