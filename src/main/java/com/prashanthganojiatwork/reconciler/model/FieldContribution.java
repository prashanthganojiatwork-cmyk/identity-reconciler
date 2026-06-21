package com.prashanthganojiatwork.reconciler.model;

/**
 * Represents an individual field's contribution to the overall confidence score.
 * Each contribution captures the field name, its assigned weight, the raw similarity
 * from the comparator, and the actual score contribution (weight x similarity).
 *
 * @param fieldName    the name of the field (e.g., "phone", "firstName")
 * @param weight       the assigned weight for this field in the scoring configuration
 * @param similarity   the raw similarity score from the comparator (0.0 to 1.0)
 * @param contribution the actual score contribution: weight x similarity
 */
public record FieldContribution(
    String fieldName,
    double weight,
    double similarity,
    double contribution
) {}
