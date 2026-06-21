package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Result of a blocking operation, containing candidate pairs to compare
 * and statistics about the blocking performance.
 *
 * @param candidatePairs pairs to compare
 * @param statistics     blocking performance metrics
 */
public record BlockingResult(
    List<CandidatePair> candidatePairs,
    BlockingStatistics statistics
) {}
