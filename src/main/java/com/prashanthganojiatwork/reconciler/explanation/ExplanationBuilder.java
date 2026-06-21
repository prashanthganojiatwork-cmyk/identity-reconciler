package com.prashanthganojiatwork.reconciler.explanation;

import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.MatchExplanation;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import com.prashanthganojiatwork.reconciler.model.ScoredMatch;

/**
 * Builds human-readable explanations for match results.
 * Takes scoring output and field comparison details to produce a comprehensive
 * explanation including per-field breakdowns, ambiguity detection, and summary text.
 *
 * <p>This interface is transport-agnostic: no Spring annotations or HTTP types.</p>
 */
public interface ExplanationBuilder {

    /**
     * Builds a complete match explanation from scoring and comparison results.
     *
     * @param scoredMatch   the scored match result from the ScoringEngine
     * @param fieldResult   the field-level comparison results from the FieldComparator
     * @param normalizedA   the normalized record from Source A
     * @param normalizedB   the normalized record from Source B
     * @return a MatchExplanation containing summary, per-field breakdowns, and ambiguity info
     */
    MatchExplanation buildExplanation(
        ScoredMatch scoredMatch,
        FieldComparisonResult fieldResult,
        NormalizedRecord normalizedA,
        NormalizedRecord normalizedB
    );
}
