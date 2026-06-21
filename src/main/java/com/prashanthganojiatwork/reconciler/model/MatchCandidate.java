package com.prashanthganojiatwork.reconciler.model;

/**
 * A proposed match between a Source A record and a Source B record,
 * including confidence scoring, classification status, and explanation.
 *
 * @param sourceARecordId  identifier of the record from Source A
 * @param sourceBRecordId  identifier of the record from Source B
 * @param confidenceScore  score between 0.0 and 1.0 representing match likelihood
 * @param status           classification: "match", "review", or "ambiguous"
 * @param requiresReview   true if the candidate falls within the review band
 * @param explanation      human-readable explanation of the match scoring
 */
public record MatchCandidate(
    String sourceARecordId,
    String sourceBRecordId,
    double confidenceScore,
    String status,
    boolean requiresReview,
    MatchExplanation explanation
) {}
