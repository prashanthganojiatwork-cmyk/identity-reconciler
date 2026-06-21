package com.prashanthganojiatwork.reconciler.model;

import java.util.Map;

/**
 * Configuration for the scoring engine, defining field weights and threshold values.
 *
 * @param fieldWeights          mapping of field name to its weight in the scoring formula
 * @param matchThreshold        the confidence score above which a candidate is considered a match (default 0.7)
 * @param reviewBandLowerBound  the lower bound of the review band (default 0.4)
 * @param conflictCapThreshold  the maximum score when a conflict cap is applied (default 0.6)
 */
public record ScoringConfig(
    Map<String, Double> fieldWeights,
    double matchThreshold,
    double reviewBandLowerBound,
    double conflictCapThreshold
) {

    /**
     * Returns the default scoring configuration with standard field weights.
     * High-entropy fields (phone, email, DOB) collectively contribute 65%.
     * Low-entropy fields (firstName, lastName, address) contribute 35%.
     *
     * @return a ScoringConfig with default weights and thresholds
     */
    public static ScoringConfig defaultConfig() {
        return new ScoringConfig(
            Map.of(
                "phone", 0.25,
                "email", 0.20,
                "dob", 0.20,
                "firstName", 0.10,
                "lastName", 0.15,
                "address", 0.10
            ),
            0.7,
            0.4,
            0.6
        );
    }
}
