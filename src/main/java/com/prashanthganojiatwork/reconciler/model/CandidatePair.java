package com.prashanthganojiatwork.reconciler.model;

import java.util.Set;

/**
 * A pair of records from opposing sources that share blocking keys
 * and should be compared for similarity.
 *
 * @param recordA           record from Source A
 * @param recordB           record from Source B
 * @param sharedBlockingKeys which blocking keys caused this pair to be compared
 */
public record CandidatePair(
    NormalizedRecord recordA,
    NormalizedRecord recordB,
    Set<String> sharedBlockingKeys
) {}
