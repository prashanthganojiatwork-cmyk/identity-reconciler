package com.prashanthganojiatwork.reconciler.model;

/**
 * Per-field comparison result containing similarity score and metadata.
 * Used by the Field Comparator to report how two field values compare.
 *
 * @param fieldName         the name of the compared field (e.g., "firstName", "phone", "email")
 * @param similarity        similarity value between 0.0 and 1.0
 * @param rawValueA         original value from Source A record
 * @param rawValueB         original value from Source B record
 * @param normalizedValueA  normalized value from Source A
 * @param normalizedValueB  normalized value from Source B
 * @param comparisonMethod  description of the algorithm used (e.g., "Jaro-Winkler", "Exact match", "Nickname resolution")
 * @param present           false if the field was missing in one or both records
 */
public record FieldScore(
    String fieldName,
    double similarity,
    String rawValueA,
    String rawValueB,
    String normalizedValueA,
    String normalizedValueB,
    String comparisonMethod,
    boolean present
) {}
