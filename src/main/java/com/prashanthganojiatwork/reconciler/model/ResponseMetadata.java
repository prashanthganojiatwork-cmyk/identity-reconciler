package com.prashanthganojiatwork.reconciler.model;

/**
 * Metadata about a reconciliation job's processing results.
 *
 * @param sourceACount         number of records in Source A
 * @param sourceBCount         number of records in Source B
 * @param matchesFound         total number of match candidates returned
 * @param flaggedForReview     number of candidates flagged for human review
 * @param processingDurationMs time taken to process the reconciliation in milliseconds
 */
public record ResponseMetadata(
    int sourceACount,
    int sourceBCount,
    int matchesFound,
    int flaggedForReview,
    long processingDurationMs
) {}
