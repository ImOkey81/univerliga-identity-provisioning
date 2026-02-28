package com.univerliga.identityprovisioning.exception;

import org.springframework.http.HttpStatus;

public class BadGatewayException extends ApiException {
    public BadGatewayException(String message) {
        super("KEYCLOAK_UNAVAILABLE", message, HttpStatus.BAD_GATEWAY);
    }
}
