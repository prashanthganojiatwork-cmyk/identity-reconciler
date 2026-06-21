package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Aggregate result of comparing all fields between two normalized records.
 * Contains per-field similarity scores and a list of fields that were absent
 * in one or both records.
 *
 * @param fieldScores   per-field comparison results
 * @param missingFields field names absent in one or both records
 */
public record FieldComparisonResult(
    List<FieldScore> fieldScores,
    List<String> missingFields
) {}
