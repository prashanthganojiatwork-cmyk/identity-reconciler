package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Response model for a reconciliation job.
 * Contains the job identifier, processing metadata, and scored match candidates.
 *
 * @param jobId     unique UUID identifying this reconciliation job
 * @param metadata  processing statistics (record counts, matches found, duration)
 * @param matches   scored match candidates, ordered by confidence score descending
 */
public record ReconciliationResponse(
    String jobId,
    ResponseMetadata metadata,
    List<MatchCandidate> matches
) {}
