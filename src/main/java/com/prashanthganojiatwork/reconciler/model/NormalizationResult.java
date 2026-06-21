package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Result of normalizing a PersonRecord.
 * Contains the normalized record and any warnings produced during normalization.
 */
public record NormalizationResult(
    NormalizedRecord normalizedRecord,
    List<NormalizationWarning> warnings
) {}
