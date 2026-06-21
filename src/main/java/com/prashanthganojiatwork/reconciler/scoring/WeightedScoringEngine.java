package com.prashanthganojiatwork.reconciler.scoring;

import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.FieldContribution;
import com.prashanthganojiatwork.reconciler.model.FieldScore;
import com.prashanthganojiatwork.reconciler.model.ScoredMatch;
import com.prashanthganojiatwork.reconciler.model.ScoringConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Weighted scoring engine that aggregates per-field similarity scores into
 * a final confidence score. Implements proportional weight redistribution
 * for missing fields and a conflict cap for single high-entropy matches
 * contradicted by other fields.
 */
@Service
public class WeightedScoringEngine implements ScoringEngine {

    /**
     * High-entropy field names that receive special conflict cap treatment.
     */
    private static final Set<String> HIGH_ENTROPY_FIELDS = Set.of("phone", "email", "dob");

    @Override
    public ScoredMatch score(FieldComparisonResult fieldResult, ScoringConfig config) {
        // Step 1: Filter to present fields only
        List<FieldScore> presentFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .toList();

        // Edge case: no fields present - return score 0.0 with empty contributions
        if (presentFields.isEmpty()) {
            return new ScoredMatch(0.0, Collections.emptyList(), false, null);
        }

        // Step 2: Compute total weight of present fields
        double totalWeight = 0.0;
        for (FieldScore field : presentFields) {
            Double weight = config.fieldWeights().get(field.fieldName());
            if (weight != null) {
                totalWeight += weight;
            }
        }

        // Edge case: total weight is zero (all present fields have no configured weight)
        if (totalWeight == 0.0) {
            return new ScoredMatch(0.0, Collections.emptyList(), false, null);
        }

        // Step 3: Redistribute weights proportionally and compute raw score
        List<FieldContribution> contributions = new ArrayList<>();
        double rawScore = 0.0;

        for (FieldScore field : presentFields) {
            Double configuredWeight = config.fieldWeights().get(field.fieldName());
            double fieldWeight = (configuredWeight != null) ? configuredWeight : 0.0;

            // Adjusted weight: proportional redistribution so weights sum to 1.0
            double adjustedWeight = fieldWeight / totalWeight;
            double contribution = adjustedWeight * field.similarity();
            rawScore += contribution;

            contributions.add(new FieldContribution(
                    field.fieldName(),
                    adjustedWeight,
                    field.similarity(),
                    contribution
            ));
        }

        // Step 6: Apply CONFLICT CAP
        boolean capped = false;
        String cappingReason = null;

        // Identify high-entropy fields with similarity >= 0.8
        List<FieldScore> highEntropyHits = presentFields.stream()
                .filter(f -> HIGH_ENTROPY_FIELDS.contains(f.fieldName()))
                .filter(f -> f.similarity() >= 0.8)
                .toList();

        // Identify any field with similarity < 0.3
        List<FieldScore> conflictingFields = presentFields.stream()
                .filter(f -> f.similarity() < 0.3)
                .toList();

        // Conflict cap applies when exactly ONE high-entropy field >= 0.8
        // AND at least one OTHER field < 0.3
        if (highEntropyHits.size() == 1 && !conflictingFields.isEmpty()) {
            String highFieldName = highEntropyHits.get(0).fieldName();
            // Find a conflicting field that is different from the high-entropy hit
            FieldScore conflictField = conflictingFields.stream()
                    .filter(f -> !f.fieldName().equals(highFieldName))
                    .findFirst()
                    .orElse(null);

            if (conflictField != null) {
                double cap = config.conflictCapThreshold();
                if (rawScore > cap) {
                    rawScore = cap;
                    capped = true;
                    cappingReason = "Conflict: " + highFieldName + " matches but "
                            + conflictField.fieldName() + " conflicts";
                }
            }
        }

        // Step 7: Round final score to 2 decimal places
        double finalScore = roundToTwoDecimalPlaces(rawScore);

        return new ScoredMatch(finalScore, contributions, capped, cappingReason);
    }

    private double roundToTwoDecimalPlaces(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
