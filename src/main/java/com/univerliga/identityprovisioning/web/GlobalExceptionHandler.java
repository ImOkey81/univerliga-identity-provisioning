package com.univerliga.identityprovisioning.web;

import com.univerliga.identityprovisioning.dto.ApiErrorResponse;
import com.univerliga.identityprovisioning.dto.ErrorDetail;
import com.univerliga.identityprovisioning.exception.ApiException;
import com.univerliga.identityprovisioning.exception.BadGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
            .map(this::toDetail)
            .toList();
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "Validation failed", details, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
            .body(error(ex.getCode(), ex.getMessage(), List.of(), ex.getStatus()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex) {
        HttpStatus status = ex instanceof BadGatewayException ? HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
            .body(error("INTERNAL_ERROR", ex.getMessage() == null ? "Unexpected error" : ex.getMessage(), List.of(), status));
    }

    private ApiErrorResponse error(String code, String message, List<ErrorDetail> details, HttpStatus status) {
        return new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message, details, RequestIdHolder.get()));
    }

    private ErrorDetail toDetail(FieldError fieldError) {
        return new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
