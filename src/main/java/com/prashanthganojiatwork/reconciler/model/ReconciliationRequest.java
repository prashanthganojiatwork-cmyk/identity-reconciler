package com.prashanthganojiatwork.reconciler.model;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Request model for a reconciliation job.
 * Contains two sets of person records (Source A and Source B) and optional threshold overrides.
 *
 * @param sourceA     1-10,000 person records from Source A
 * @param sourceB     1-10,000 person records from Source B
 * @param thresholds  optional threshold overrides for this request (null uses defaults)
 */
public record ReconciliationRequest(
    List<PersonRecord> sourceA,
    List<PersonRecord> sourceB,
    @Nullable ThresholdConfig thresholds
) {}
