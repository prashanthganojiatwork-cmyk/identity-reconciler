package com.prashanthganojiatwork.reconciler.model;

/**
 * Per-field detailed breakdown used in match explanations.
 * Shows how each field was compared and what it contributed to the final score.
 *
 * @param fieldName              the name of the compared field (e.g., "firstName", "phone")
 * @param rawValueA              original value from Source A
 * @param rawValueB              original value from Source B
 * @param normalizationApplied   description of normalization done (e.g., "Lowercase, whitespace collapse")
 * @param similarityMethod       algorithm used for comparison (e.g., "Jaro-Winkler", "Exact match")
 * @param similarityResult       similarity score between 0.0 and 1.0
 * @param scoreContribution      weighted contribution to the final confidence score
 * @param present                false if the field was absent in one or both records
 */
public record FieldBreakdown(
    String fieldName,
    String rawValueA,
    String rawValueB,
    String normalizationApplied,
    String similarityMethod,
    double similarityResult,
    double scoreContribution,
    boolean present
) {}
