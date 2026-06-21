package com.prashanthganojiatwork.reconciler.model;

import jakarta.annotation.Nullable;

/**
 * Optional threshold configuration for a reconciliation request.
 * When null, defaults are applied: matchThreshold=0.7, reviewBandLowerBound=0.4.
 *
 * @param matchThreshold        confidence score boundary above which a candidate is a positive match (default 0.7)
 * @param reviewBandLowerBound  lower bound of the review band; candidates below this are excluded (default 0.4)
 */
public record ThresholdConfig(
    @Nullable Double matchThreshold,
    @Nullable Double reviewBandLowerBound
) {}
