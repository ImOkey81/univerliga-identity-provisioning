package com.univerliga.identityprovisioning.controller;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.dto.ApiResponse;
import com.univerliga.identityprovisioning.dto.SystemVersionDto;
import com.univerliga.identityprovisioning.web.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ApiResponseFactory responseFactory;
    private final AppProperties appProperties;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.build-version:0.1.0}")
    private String version;

    @GetMapping("/version")
    public ApiResponse<SystemVersionDto> version() {
        return responseFactory.success(new SystemVersionDto(appName, version, appProperties.getProvisioning().getMode()));
    }
}
