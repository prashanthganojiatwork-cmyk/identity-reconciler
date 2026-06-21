package com.prashanthganojiatwork.reconciler.scoring;

import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.ScoredMatch;
import com.prashanthganojiatwork.reconciler.model.ScoringConfig;

/**
 * Engine responsible for aggregating per-field similarity scores into a final
 * confidence score. Applies weighted scoring, missing-field redistribution,
 * and conflict capping logic.
 *
 * <p>Implementations must remain transport-agnostic: no Spring annotations
 * or HTTP types should appear in implementations of this interface.</p>
 */
public interface ScoringEngine {

    /**
     * Computes a scored match result from field comparison data and scoring configuration.
     *
     * @param fieldResult the per-field comparison results from the field comparator
     * @param config      the scoring configuration with weights and thresholds
     * @return a ScoredMatch containing the confidence score, per-field contributions,
     *         and any conflict cap information
     */
    ScoredMatch score(FieldComparisonResult fieldResult, ScoringConfig config);
}
