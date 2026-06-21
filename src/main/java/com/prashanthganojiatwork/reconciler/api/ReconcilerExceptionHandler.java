package com.prashanthganojiatwork.reconciler.api;

import com.prashanthganojiatwork.reconciler.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for the reconciler API.
 * Converts exceptions to structured ErrorResponse DTOs with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class ReconcilerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReconcilerExceptionHandler.class);

    /**
     * Handle custom validation exceptions (empty dataset, invalid threshold, etc.).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(ex.getCode(), ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle malformed JSON or unreadable HTTP message body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR",
            "Malformed request body: unable to parse JSON", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle payload too large (multipart max size exceeded).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ErrorResponse error = new ErrorResponse("PAYLOAD_TOO_LARGE",
            "Request payload exceeds the maximum allowed size of 10MB", null);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * Catch-all handler for unexpected internal errors.
     * Does not expose internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error during reconciliation", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR",
            "An unexpected error occurred", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
