package com.pm.appointmentservice.exception;

import com.pm.appointmentservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> validation(Exception ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (ex instanceof MethodArgumentNotValidException methodException) {
            methodException.getBindingResult().getAllErrors().forEach(error -> {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
                fields.putIfAbsent(field, error.getDefaultMessage());
            });
        } else if (ex instanceof ConstraintViolationException constraintException) {
            constraintException.getConstraintViolations().forEach(violation ->
                    fields.put(violation.getPropertyPath().toString(), violation.getMessage()));
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Input validation failed", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "The request body is missing or malformed", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected request failure", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                               HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
    }
}
