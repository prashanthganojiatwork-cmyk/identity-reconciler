package com.prashanthganojiatwork.reconciler.orchestrator;

import com.prashanthganojiatwork.reconciler.blocking.BlockingEngine;
import com.prashanthganojiatwork.reconciler.comparator.FieldComparator;
import com.prashanthganojiatwork.reconciler.explanation.ExplanationBuilder;
import com.prashanthganojiatwork.reconciler.model.*;
import com.prashanthganojiatwork.reconciler.normalizer.Normalizer;
import com.prashanthganojiatwork.reconciler.scoring.ScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ReconciliationOrchestrator} that wires all pipeline
 * components together: Normalizer → BlockingEngine → FieldComparator → ScoringEngine → ExplanationBuilder.
 *
 * <p>Pipeline steps:
 * <ol>
 *   <li>Resolve thresholds (use request overrides or defaults)</li>
 *   <li>Filter out all-empty records (Req 9.4) — log warning for each</li>
 *   <li>Normalize all records</li>
 *   <li>Generate candidate pairs via blocking</li>
 *   <li>For each candidate pair: compare → score → explain</li>
 *   <li>Apply threshold filtering (exclude candidates below review band lower bound)</li>
 *   <li>Sort by confidence descending, cap at 100 per Source_A record</li>
 *   <li>Build response with metadata</li>
 * </ol>
 *
 * <p>Satisfies Requirements: 1.3 (missing field tolerance), 3.5 (ordering/cap),
 * 5.1-5.4 (thresholds), 7.3 (metadata), 9.1-9.5 (ambiguous handling), 10.4 (DI).</p>
 */
@Service
public class DefaultReconciliationOrchestrator implements ReconciliationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultReconciliationOrchestrator.class);
    private static final int MAX_MATCHES_PER_SOURCE_A_RECORD = 100;
    private static final double DEFAULT_MATCH_THRESHOLD = 0.7;
    private static final double DEFAULT_REVIEW_BAND_LOWER_BOUND = 0.4;

    private final Normalizer normalizer;
    private final FieldComparator fieldComparator;
    private final BlockingEngine blockingEngine;
    private final ScoringEngine scoringEngine;
    private final ExplanationBuilder explanationBuilder;

    public DefaultReconciliationOrchestrator(
            Normalizer normalizer,
            FieldComparator fieldComparator,
            BlockingEngine blockingEngine,
            ScoringEngine scoringEngine,
            ExplanationBuilder explanationBuilder) {
        this.normalizer = normalizer;
        this.fieldComparator = fieldComparator;
        this.blockingEngine = blockingEngine;
        this.scoringEngine = scoringEngine;
        this.explanationBuilder = explanationBuilder;
    }

    @Override
    public ReconciliationResponse reconcile(ReconciliationRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Resolve thresholds (use request overrides or defaults)
        double matchThreshold = DEFAULT_MATCH_THRESHOLD;
        double reviewLowerBound = DEFAULT_REVIEW_BAND_LOWER_BOUND;

        if (request.thresholds() != null) {
            if (request.thresholds().matchThreshold() != null) {
                matchThreshold = request.thresholds().matchThreshold();
            }
            if (request.thresholds().reviewBandLowerBound() != null) {
                reviewLowerBound = request.thresholds().reviewBandLowerBound();
            }
        }

        ScoringConfig scoringConfig = new ScoringConfig(
                ScoringConfig.defaultConfig().fieldWeights(),
                matchThreshold,
                reviewLowerBound,
                ScoringConfig.defaultConfig().conflictCapThreshold()
        );

        // 2. Filter out all-empty records (Req 9.4) — log warning for each
        List<PersonRecord> validSourceA = filterEmptyRecords(request.sourceA(), "A");
        List<PersonRecord> validSourceB = filterEmptyRecords(request.sourceB(), "B");

        // 3. Normalize all records
        List<NormalizedRecord> normalizedA = normalizer.normalizeBatch(validSourceA).stream()
                .map(NormalizationResult::normalizedRecord)
                .collect(Collectors.toList());

        List<NormalizedRecord> normalizedB = normalizer.normalizeBatch(validSourceB).stream()
                .map(NormalizationResult::normalizedRecord)
                .collect(Collectors.toList());

        // 4. Generate candidate pairs via blocking
        BlockingResult blockingResult = blockingEngine.generateCandidatePairs(normalizedA, normalizedB);

        // 5. For each candidate pair: compare → score → explain
        List<MatchCandidate> matches = new ArrayList<>();

        for (CandidatePair pair : blockingResult.candidatePairs()) {
            FieldComparisonResult fieldResult = fieldComparator.compare(pair.recordA(), pair.recordB());
            ScoredMatch scoredMatch = scoringEngine.score(fieldResult, scoringConfig);
            MatchExplanation explanation = explanationBuilder.buildExplanation(
                    scoredMatch, fieldResult, pair.recordA(), pair.recordB());

            // 6. Apply threshold filtering — exclude candidates below review band lower bound
            if (scoredMatch.confidenceScore() >= reviewLowerBound) {
                String status = determineStatus(
                        scoredMatch.confidenceScore(), matchThreshold, reviewLowerBound, explanation.ambiguous());
                boolean requiresReview = scoredMatch.confidenceScore() < matchThreshold;

                matches.add(new MatchCandidate(
                        pair.recordA().id(),
                        pair.recordB().id(),
                        scoredMatch.confidenceScore(),
                        status,
                        requiresReview,
                        explanation
                ));
            }
        }

        // 7. Sort by confidence descending, cap at 100 per Source_A record
        matches = sortAndCap(matches);

        // 8. Build response with metadata
        long duration = System.currentTimeMillis() - startTime;
        int flaggedForReview = (int) matches.stream().filter(MatchCandidate::requiresReview).count();

        ResponseMetadata metadata = new ResponseMetadata(
                request.sourceA().size(),
                request.sourceB().size(),
                matches.size(),
                flaggedForReview,
                duration
        );

        return new ReconciliationResponse(
                UUID.randomUUID().toString(),
                metadata,
                matches
        );
    }

    /**
     * Determines the match status based on confidence score, thresholds, and ambiguity.
     *
     * @param confidenceScore the match confidence score
     * @param matchThreshold  the threshold above which a match is confirmed
     * @param reviewLowerBound the lower bound of the review band
     * @param ambiguous        whether the explanation identified this as ambiguous
     * @return "match", "ambiguous", or "review"
     */
    private String determineStatus(double confidenceScore, double matchThreshold,
                                   double reviewLowerBound, boolean ambiguous) {
        if (confidenceScore >= matchThreshold) {
            return "match";
        }
        // In review band [reviewLowerBound, matchThreshold)
        if (ambiguous) {
            return "ambiguous";
        }
        return "review";
    }

    /**
     * Sorts match candidates by confidence score descending, then caps at 100 per Source_A record.
     */
    private List<MatchCandidate> sortAndCap(List<MatchCandidate> matches) {
        // Sort all matches by confidence score descending
        matches.sort(Comparator.comparingDouble(MatchCandidate::confidenceScore).reversed());

        // Group by sourceARecordId, keep at most 100 per group (already sorted within group)
        Map<String, List<MatchCandidate>> grouped = new LinkedHashMap<>();
        for (MatchCandidate match : matches) {
            grouped.computeIfAbsent(match.sourceARecordId(), k -> new ArrayList<>()).add(match);
        }

        List<MatchCandidate> capped = new ArrayList<>();
        for (List<MatchCandidate> group : grouped.values()) {
            int limit = Math.min(group.size(), MAX_MATCHES_PER_SOURCE_A_RECORD);
            capped.addAll(group.subList(0, limit));
        }

        // Re-sort the final list by confidence descending
        capped.sort(Comparator.comparingDouble(MatchCandidate::confidenceScore).reversed());

        return capped;
    }

    /**
     * Filters out all-empty records from a source list.
     * A record is considered "all-empty" if none of its matchable fields
     * (firstname, lastname, phone, email, dateOfBirth, address) are non-null and non-empty.
     *
     * @param records  the list of person records to filter
     * @param sourceId identifier for logging (e.g., "A" or "B")
     * @return list of records with at least one non-empty matchable field
     */
    private List<PersonRecord> filterEmptyRecords(List<PersonRecord> records, String sourceId) {
        List<PersonRecord> valid = new ArrayList<>();
        for (PersonRecord record : records) {
            if (hasAtLeastOneField(record)) {
                valid.add(record);
            } else {
                log.warn("Excluding all-empty record '{}' from Source {} — no matchable fields present",
                        record.id(), sourceId);
            }
        }
        return valid;
    }

    /**
     * Returns true if any of firstname, lastname, phone, email, dateOfBirth, or address
     * is non-null and non-empty.
     */
    private boolean hasAtLeastOneField(PersonRecord record) {
        if (isNonEmpty(record.firstname())) return true;
        if (isNonEmpty(record.lastname())) return true;
        if (isNonEmpty(record.phone())) return true;
        if (isNonEmpty(record.email())) return true;
        if (isNonEmpty(record.dateOfBirth())) return true;
        if (record.address() != null && hasNonEmptyAddressField(record.address())) return true;
        return false;
    }

    /**
     * Returns true if the string is non-null and not blank.
     */
    private boolean isNonEmpty(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Returns true if any field in the address is non-null and non-empty.
     */
    private boolean hasNonEmptyAddressField(Address address) {
        return isNonEmpty(address.streetLine1())
                || isNonEmpty(address.streetLine2())
                || isNonEmpty(address.city())
                || isNonEmpty(address.stateCode())
                || isNonEmpty(address.postalCode())
                || isNonEmpty(address.countryCode());
    }
}
