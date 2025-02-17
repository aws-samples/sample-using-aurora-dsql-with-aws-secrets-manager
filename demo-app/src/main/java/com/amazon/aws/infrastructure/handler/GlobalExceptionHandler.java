// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.handler;

import com.amazon.aws.infrastructure.dto.ErrorResponse;
import com.amazon.aws.infrastructure.exception.AddressValidationException;
import com.amazon.aws.infrastructure.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the classes annotated with @RestController.
 * This is meant to provide default shared error handling for API endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.error("Method argument not valid exception", ex);

        Map<String, List<String>> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), Collections.singletonList(error.getDefaultMessage())));

        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value()).message("Method argument not valid").errors(errors).path(request.getDescription(false)).build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {

        log.error("Constraint violation exception", ex);

        Map<String, List<String>> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> errors.put(violation.getPropertyPath().toString(), Collections.singletonList(violation.getMessage())));

        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value()).message("Constraint violation").errors(errors).path(request.getDescription(false)).build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {

        log.error("Resource not found exception", ex);

        Map<String, List<String>> errors = new HashMap<>();
        errors.put("resource-not-found", Collections.singletonList("Resource not found. Please check the request."));

        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value()).message("Resource not found").errors(errors).path(request.getDescription(false)).build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(AddressValidationException.class)
    public ResponseEntity<Object> handleAddressValidationException(AddressValidationException ex, WebRequest request) {

        log.error("Address validation exception", ex);

        Map<String, List<String>> errors = new HashMap<>();
        errors.put("address-validation-error", Collections.singletonList("Address validation failed. Please check the request."));

        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value()).message("Address validation failed").errors(errors).path(request.getDescription(false)).build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {

        log.error("Encountered runtime exception", ex);

        Map<String, List<String>> errors = new HashMap<>();
        errors.put("runtime-error", Collections.singletonList(ex.getMessage()));

        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).message(ex.getMessage()).errors(errors).path(request.getDescription(false)).build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
