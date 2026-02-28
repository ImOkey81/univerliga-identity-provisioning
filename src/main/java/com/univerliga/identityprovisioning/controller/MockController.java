package com.univerliga.identityprovisioning.controller;

import com.univerliga.identityprovisioning.config.AppProperties;
import com.univerliga.identityprovisioning.domain.EventType;
import com.univerliga.identityprovisioning.domain.ProcessedEventStatus;
import com.univerliga.identityprovisioning.dto.ApiResponse;
import com.univerliga.identityprovisioning.dto.CrmPayload;
import com.univerliga.identityprovisioning.dto.MockEventAcceptedResponse;
import com.univerliga.identityprovisioning.dto.MockEventRequest;
import com.univerliga.identityprovisioning.dto.SeedResponse;
import com.univerliga.identityprovisioning.messaging.RabbitEventPublisher;
import com.univerliga.identityprovisioning.service.EventProcessingService;
import com.univerliga.identityprovisioning.web.ApiResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mock")
@RequiredArgsConstructor
public class MockController {

    private final EventProcessingService eventProcessingService;
    private final RabbitEventPublisher rabbitEventPublisher;
    private final ApiResponseFactory responseFactory;
    private final AppProperties properties;

    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MockEventAcceptedResponse>> inject(@Valid @RequestBody MockEventRequest request) {
        if ("broker".equalsIgnoreCase(properties.getProvisioning().getMode())) {
            rabbitEventPublisher.publish(request);
            return ResponseEntity.accepted().body(responseFactory.success(new MockEventAcceptedResponse(true, request.eventId().toString(), "QUEUED")));
        }

        ProcessedEventStatus result = eventProcessingService.process(request);
        return ResponseEntity.status(HttpStatus.OK)
            .body(responseFactory.success(new MockEventAcceptedResponse(true, request.eventId().toString(), result.name())));
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SeedResponse> seed() {
        for (int i = 1; i <= 10; i++) {
            MockEventRequest event = new MockEventRequest(
                UUID.randomUUID(),
                EventType.PersonCreated,
                OffsetDateTime.now(),
                new CrmPayload("p_" + i, "user" + i, "user" + i + "@example.com", "User " + i, Set.of("ROLE_EMPLOYEE"), true)
            );

            if ("broker".equalsIgnoreCase(properties.getProvisioning().getMode())) {
                rabbitEventPublisher.publish(event);
            } else {
                eventProcessingService.process(event);
            }
        }
        return responseFactory.success(new SeedResponse(10));
    }
}
