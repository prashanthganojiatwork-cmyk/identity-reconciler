package com.prashanthganojiatwork.reconciler.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Structured error response returned by the API for validation and runtime errors.
 *
 * @param code        machine-readable error code (e.g., EMPTY_DATASET, VALIDATION_ERROR)
 * @param message     human-readable description of the error
 * @param fieldErrors optional list of specific field-level errors
 */
public record ErrorResponse(
    String code,
    String message,
    @Nullable List<String> fieldErrors
) {}
