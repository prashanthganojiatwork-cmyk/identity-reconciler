package com.prashanthganojiatwork.reconciler.blocking;

import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;

import java.util.Set;

/**
 * Generates blocking keys for a normalized record.
 * Records sharing blocking keys will be placed in the same block for comparison.
 */
public interface BlockingKeyGenerator {

    /**
     * Generate blocking keys for the given record.
     *
     * @param record the normalized record to generate keys for
     * @return set of blocking keys; empty set if no keys can be generated
     */
    Set<String> generateKeys(NormalizedRecord record);
}
