package com.prashanthganojiatwork.reconciler.blocking;

import com.prashanthganojiatwork.reconciler.model.BlockingResult;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;

import java.util.List;

/**
 * Generates candidate pairs from two sources of normalized records using blocking
 * strategies to avoid exhaustive O(n×m) comparison.
 */
public interface BlockingEngine {

    /**
     * Generate candidate pairs by grouping records into blocks using shared blocking keys.
     *
     * @param sourceA normalized records from Source A
     * @param sourceB normalized records from Source B
     * @return blocking result containing candidate pairs and performance statistics
     */
    BlockingResult generateCandidatePairs(List<NormalizedRecord> sourceA, List<NormalizedRecord> sourceB);
}
