package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Result of the scoring engine's evaluation of a candidate pair.
 * Contains the final confidence score (0.0 to 1.0, rounded to 2 decimal places),
 * per-field contributions, and conflict cap information.
 *
 * @param confidenceScore the overall match confidence between 0.0 and 1.0, rounded to 2 decimal places
 * @param contributions   per-field score contributions showing how each field impacted the score
 * @param capped          true if a conflict cap was applied to limit the score
 * @param cappingReason   null if not capped; otherwise describes why the cap was applied
 */
public record ScoredMatch(
    double confidenceScore,
    List<FieldContribution> contributions,
    boolean capped,
    String cappingReason
) {}
