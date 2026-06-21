package com.prashanthganojiatwork.reconciler.comparator;

import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;

/**
 * Component interface for per-field similarity computation between two normalized records.
 * Compares all available fields and produces a structured comparison result.
 *
 * <p>This interface is transport-agnostic: it uses no Spring annotations, HTTP types,
 * or in-memory collection internals, ensuring the contract can be used across
 * different deployment topologies (Req 10.5).</p>
 */
public interface FieldComparator {

    /**
     * Compares all fields between two normalized records and produces per-field
     * similarity scores along with a list of missing fields.
     *
     * @param recordA the first normalized record (from Source A)
     * @param recordB the second normalized record (from Source B)
     * @return the comparison result containing per-field scores and missing field list
     */
    FieldComparisonResult compare(NormalizedRecord recordA, NormalizedRecord recordB);
}
