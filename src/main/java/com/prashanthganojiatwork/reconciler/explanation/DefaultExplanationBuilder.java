package com.prashanthganojiatwork.reconciler.explanation;

import com.prashanthganojiatwork.reconciler.model.FieldBreakdown;
import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.FieldContribution;
import com.prashanthganojiatwork.reconciler.model.FieldScore;
import com.prashanthganojiatwork.reconciler.model.MatchExplanation;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import com.prashanthganojiatwork.reconciler.model.ScoredMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ExplanationBuilder} that produces human-readable
 * match explanations with per-field breakdowns, ambiguity detection, and summary text.
 *
 * <p>Satisfies Requirements 4.1-4.7, 9.5, 10.1, 10.4.</p>
 */
@Service
public class DefaultExplanationBuilder implements ExplanationBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultExplanationBuilder.class);

    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double REVIEW_BAND_UPPER = 0.7;
    private static final double REVIEW_BAND_LOWER = 0.4;
    private static final double AMBIGUITY_HIGH_SIMILARITY = 0.7;
    private static final double AMBIGUITY_LOW_SIMILARITY = 0.4;
    private static final double SCORE_TOLERANCE = 0.001;

    /**
     * Maps field names to their normalization description.
     */
    private static final Map<String, String> NORMALIZATION_DESCRIPTIONS = Map.of(
            "firstName", "Lowercase, whitespace collapse, nickname resolution",
            "lastName", "Lowercase, whitespace collapse, retain hyphens/apostrophes",
            "phone", "Strip non-digits, prepend country code",
            "email", "Lowercase, plus-addressing removed",
            "address", "Lowercase, USPS abbreviation expansion, whitespace collapse",
            "dob", "Multi-format parse to ISO 8601 (YYYY-MM-DD)"
    );

    @Override
    public MatchExplanation buildExplanation(
            ScoredMatch scoredMatch,
            FieldComparisonResult fieldResult,
            NormalizedRecord normalizedA,
            NormalizedRecord normalizedB) {

        // Build contribution lookup by field name
        Map<String, FieldContribution> contributionMap = scoredMatch.contributions().stream()
                .collect(Collectors.toMap(FieldContribution::fieldName, fc -> fc, (a, b) -> a));

        // Step 1: Build per-field breakdowns
        List<FieldBreakdown> fieldBreakdowns = buildFieldBreakdowns(fieldResult, contributionMap);

        // Step 2: Verify score composition integrity
        verifyScoreComposition(scoredMatch.confidenceScore(), fieldBreakdowns);

        // Step 3: Identify ambiguity
        boolean ambiguous = isAmbiguous(scoredMatch.confidenceScore(), fieldResult);
        List<String> ambiguityReasons = ambiguous
                ? buildAmbiguityReasons(fieldResult)
                : List.of();

        // Step 4: Generate summary
        String summary = generateSummary(scoredMatch.confidenceScore(), fieldResult, ambiguous);

        return new MatchExplanation(
                summary,
                fieldBreakdowns,
                scoredMatch.confidenceScore(),
                ambiguous,
                ambiguityReasons
        );
    }

    /**
     * Builds per-field breakdown entries from field comparison results and score contributions.
     */
    private List<FieldBreakdown> buildFieldBreakdowns(
            FieldComparisonResult fieldResult,
            Map<String, FieldContribution> contributionMap) {

        List<FieldBreakdown> breakdowns = new ArrayList<>();

        for (FieldScore fieldScore : fieldResult.fieldScores()) {
            if (fieldScore.present()) {
                // Present field: look up its contribution
                FieldContribution contribution = contributionMap.get(fieldScore.fieldName());
                double scoreContribution = (contribution != null) ? contribution.contribution() : 0.0;

                breakdowns.add(new FieldBreakdown(
                        fieldScore.fieldName(),
                        fieldScore.rawValueA(),
                        fieldScore.rawValueB(),
                        getNormalizationDescription(fieldScore.fieldName()),
                        fieldScore.comparisonMethod(),
                        fieldScore.similarity(),
                        scoreContribution,
                        true
                ));
            } else {
                // Missing/absent field
                breakdowns.add(new FieldBreakdown(
                        fieldScore.fieldName(),
                        fieldScore.rawValueA(),
                        fieldScore.rawValueB(),
                        "Field absent - excluded from scoring",
                        fieldScore.comparisonMethod(),
                        0.0,
                        0.0,
                        false
                ));
            }
        }

        return breakdowns;
    }

    /**
     * Verifies that the sum of score contributions matches the total confidence score within tolerance.
     * Logs a warning if the integrity check fails.
     */
    private void verifyScoreComposition(double totalScore, List<FieldBreakdown> breakdowns) {
        double sumContributions = breakdowns.stream()
                .mapToDouble(FieldBreakdown::scoreContribution)
                .sum();

        if (Math.abs(sumContributions - totalScore) > SCORE_TOLERANCE) {
            log.warn("Score composition mismatch: totalScore={}, sumContributions={}, difference={}",
                    totalScore, sumContributions, Math.abs(sumContributions - totalScore));
        }
    }

    /**
     * Determines if a match is ambiguous based on:
     * - Confidence score is in the review band (0.4 to 0.7)
     * - At least one field has similarity >= 0.7
     * - At least one field has similarity <= 0.4
     */
    private boolean isAmbiguous(double confidenceScore, FieldComparisonResult fieldResult) {
        if (confidenceScore < REVIEW_BAND_LOWER || confidenceScore > REVIEW_BAND_UPPER) {
            return false;
        }

        boolean hasHighSimilarity = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .anyMatch(f -> f.similarity() >= AMBIGUITY_HIGH_SIMILARITY);

        boolean hasLowSimilarity = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .anyMatch(f -> f.similarity() <= AMBIGUITY_LOW_SIMILARITY);

        return hasHighSimilarity && hasLowSimilarity;
    }

    /**
     * Builds human-readable ambiguity reasons listing conflicting fields.
     * Example: "Phone matches (0.95) but name conflicts (0.20)"
     */
    private List<String> buildAmbiguityReasons(FieldComparisonResult fieldResult) {
        List<String> reasons = new ArrayList<>();

        List<FieldScore> highFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .filter(f -> f.similarity() >= AMBIGUITY_HIGH_SIMILARITY)
                .toList();

        List<FieldScore> lowFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .filter(f -> f.similarity() <= AMBIGUITY_LOW_SIMILARITY)
                .toList();

        for (FieldScore high : highFields) {
            for (FieldScore low : lowFields) {
                reasons.add(String.format("%s matches (%.2f) but %s conflicts (%.2f)",
                        high.fieldName(), high.similarity(),
                        low.fieldName(), low.similarity()));
            }
        }

        return reasons;
    }

    /**
     * Generates a human-readable summary based on confidence level.
     * - High confidence (>= 0.8): "Strong match: [top 2 fields] agree with high similarity."
     * - Review band (0.4-0.7): "Ambiguous match: [agreeing] support match, but [conflicting] differ. Requires human review."
     * - Low confidence (< 0.4): "Weak match: limited agreement across compared fields."
     *
     * Summary is truncated to 500 characters if needed.
     */
    private String generateSummary(double confidenceScore, FieldComparisonResult fieldResult, boolean ambiguous) {
        String summary;

        if (confidenceScore >= HIGH_CONFIDENCE_THRESHOLD) {
            summary = generateHighConfidenceSummary(fieldResult);
        } else if (confidenceScore >= REVIEW_BAND_LOWER) {
            summary = generateReviewBandSummary(fieldResult);
        } else {
            summary = "Weak match: limited agreement across compared fields.";
        }

        // Truncate to 500 chars if needed
        if (summary.length() > MAX_SUMMARY_LENGTH) {
            summary = summary.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
        }

        return summary;
    }

    /**
     * Generates summary for high-confidence matches (>= 0.8).
     * Includes the top 2 contributing fields by similarity.
     */
    private String generateHighConfidenceSummary(FieldComparisonResult fieldResult) {
        List<String> topFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .sorted(Comparator.comparingDouble(FieldScore::similarity).reversed())
                .limit(2)
                .map(FieldScore::fieldName)
                .toList();

        String fieldNames = String.join(", ", topFields);
        return String.format("Strong match: %s agree with high similarity.", fieldNames);
    }

    /**
     * Generates summary for review band matches (0.4-0.7).
     * Lists agreeing fields (similarity >= 0.7) and conflicting fields (similarity <= 0.4).
     */
    private String generateReviewBandSummary(FieldComparisonResult fieldResult) {
        List<String> agreeingFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .filter(f -> f.similarity() >= AMBIGUITY_HIGH_SIMILARITY)
                .map(FieldScore::fieldName)
                .toList();

        List<String> conflictingFields = fieldResult.fieldScores().stream()
                .filter(FieldScore::present)
                .filter(f -> f.similarity() <= AMBIGUITY_LOW_SIMILARITY)
                .map(FieldScore::fieldName)
                .toList();

        if (agreeingFields.isEmpty() && conflictingFields.isEmpty()) {
            return "Ambiguous match: partial agreement across fields. Requires human review.";
        }

        StringBuilder sb = new StringBuilder("Ambiguous match: ");

        if (!agreeingFields.isEmpty()) {
            sb.append(String.join(", ", agreeingFields)).append(" support match");
        }

        if (!conflictingFields.isEmpty()) {
            if (!agreeingFields.isEmpty()) {
                sb.append(", but ");
            }
            sb.append(String.join(", ", conflictingFields)).append(" differ");
        }

        sb.append(". Requires human review.");
        return sb.toString();
    }

    /**
     * Returns the normalization description for a given field name.
     * Falls back to a generic description for unknown fields.
     */
    private String getNormalizationDescription(String fieldName) {
        return NORMALIZATION_DESCRIPTIONS.getOrDefault(fieldName, "Standard normalization applied");
    }
}
