package com.prashanthganojiatwork.reconciler.model;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Canonicalized person record produced by the Normalizer.
 * Contains normalized field values and preserves raw values for explanation output.
 */
public record NormalizedRecord(
    String id,
    String sourceId,
    @Nullable String normalizedFirstName,
    @Nullable String normalizedLastName,
    @Nullable String normalizedPhone,
    @Nullable String normalizedEmail,
    @Nullable String normalizedAddress,
    @Nullable LocalDate normalizedDob,
    List<String> alternateNames,
    Map<String, String> rawValues
) {}
