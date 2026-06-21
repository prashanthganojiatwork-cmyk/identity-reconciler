package com.prashanthganojiatwork.reconciler.model;

/**
 * Warning produced when a field value cannot be parsed or normalized.
 * Included in the match explanation to indicate normalization issues.
 */
public record NormalizationWarning(
    String fieldName,
    String originalValue,
    String reason
) {}
