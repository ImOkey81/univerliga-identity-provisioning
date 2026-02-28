package com.univerliga.identityprovisioning.controller;

import com.univerliga.identityprovisioning.domain.ProvisioningStatus;
import com.univerliga.identityprovisioning.dto.ApiResponse;
import com.univerliga.identityprovisioning.dto.PagedUsersResponse;
import com.univerliga.identityprovisioning.dto.ProcessedEventDto;
import com.univerliga.identityprovisioning.dto.ReconcileResponse;
import com.univerliga.identityprovisioning.dto.UserStatusDto;
import com.univerliga.identityprovisioning.dto.UserToggleResponse;
import com.univerliga.identityprovisioning.dto.UserUpdateRequest;
import com.univerliga.identityprovisioning.dto.UserUpsertRequest;
import com.univerliga.identityprovisioning.service.EventProcessingService;
import com.univerliga.identityprovisioning.service.ProvisioningService;
import com.univerliga.identityprovisioning.web.ApiResponseFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provisioning")
@RequiredArgsConstructor
@Validated
public class ProvisioningController {

    private final ProvisioningService provisioningService;
    private final EventProcessingService eventProcessingService;
    private final ApiResponseFactory responseFactory;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserStatusDto>> create(@Valid @RequestBody UserUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseFactory.success(provisioningService.createOrLink(request)));
    }

    @PutMapping("/users/{personId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserStatusDto> update(@PathVariable String personId, @Valid @RequestBody UserUpdateRequest request) {
        return responseFactory.success(provisioningService.update(personId, request));
    }

    @PostMapping("/users/{personId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserToggleResponse> disable(@PathVariable String personId) {
        return responseFactory.success(provisioningService.toggle(personId, false));
    }

    @PostMapping("/users/{personId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserToggleResponse> enable(@PathVariable String personId) {
        return responseFactory.success(provisioningService.toggle(personId, true));
    }

    @PostMapping("/users/{personId}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReconcileResponse> reconcile(@PathVariable String personId) {
        return responseFactory.success(provisioningService.reconcile(personId));
    }

    @GetMapping("/users/{personId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<UserStatusDto> get(@PathVariable String personId) {
        return responseFactory.success(provisioningService.get(personId));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<PagedUsersResponse> list(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) ProvisioningStatus status,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return responseFactory.success(provisioningService.list(query, status, page, size));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<ProcessedEventDto> getEvent(@PathVariable UUID eventId) {
        return responseFactory.success(eventProcessingService.get(eventId));
    }
}
