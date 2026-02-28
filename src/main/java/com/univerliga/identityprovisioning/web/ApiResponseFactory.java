package com.univerliga.identityprovisioning.web;

import com.univerliga.identityprovisioning.dto.ApiMeta;
import com.univerliga.identityprovisioning.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ApiResponseFactory {

    private final String version;

    public ApiResponseFactory(@Value("${app.api-version:v1}") String version) {
        this.version = version;
    }

    public <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .data(data)
            .meta(ApiMeta.builder()
                .requestId(RequestIdHolder.get())
                .timestamp(OffsetDateTime.now())
                .version(version)
                .build())
            .build();
    }
}
