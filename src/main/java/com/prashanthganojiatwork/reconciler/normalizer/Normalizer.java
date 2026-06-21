package com.prashanthganojiatwork.reconciler.normalizer;

import com.prashanthganojiatwork.reconciler.model.NormalizationResult;
import com.prashanthganojiatwork.reconciler.model.PersonRecord;

import java.util.List;

/**
 * Component interface for field normalization.
 * Transforms raw PersonRecord field values into canonical forms before comparison.
 *
 * <p>This interface is transport-agnostic: it uses no Spring annotations, HTTP types,
 * or in-memory collection internals, ensuring the contract can be used across
 * different deployment topologies (Req 10.5).</p>
 */
public interface Normalizer {

    /**
     * Normalizes all fields of a single PersonRecord.
     *
     * @param record the raw person record to normalize
     * @return the normalization result containing the normalized record and any warnings
     */
    NormalizationResult normalize(PersonRecord record);

    /**
     * Normalizes a batch of PersonRecords.
     *
     * @param records the list of raw person records to normalize
     * @return a list of normalization results, one per input record
     */
    List<NormalizationResult> normalizeBatch(List<PersonRecord> records);
}
