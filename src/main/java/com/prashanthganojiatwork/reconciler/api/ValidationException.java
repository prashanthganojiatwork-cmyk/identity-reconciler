package com.prashanthganojiatwork.reconciler.api;

/**
 * Custom validation exception carrying an error code and message
 * for structured error responses from the reconciler API.
 */
public class ValidationException extends RuntimeException {

    private final String code;

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
