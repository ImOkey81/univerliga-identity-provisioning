package com.univerliga.identityprovisioning.dto;

import java.util.List;

public record ApiErrorResponse(ApiError error) {

    public record ApiError(
        String code,
        String message,
        List<ErrorDetail> details,
        String requestId
    ) {
    }
}
