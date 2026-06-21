package com.prashanthganojiatwork.reconciler.orchestrator;

import com.prashanthganojiatwork.reconciler.model.ReconciliationRequest;
import com.prashanthganojiatwork.reconciler.model.ReconciliationResponse;

/**
 * Orchestrates the full reconciliation pipeline: normalization, blocking,
 * field comparison, scoring, and explanation building.
 *
 * This interface is transport-agnostic — it does not reference Spring annotations,
 * HTTP types, or any framework-specific constructs. It defines the core reconciliation
 * contract that can be invoked from any transport layer (REST, gRPC, CLI, etc.).
 */
public interface ReconciliationOrchestrator {

    /**
     * Executes a full reconciliation between two sets of person records.
     *
     * @param request the reconciliation request containing Source A records,
     *                Source B records, and optional threshold overrides
     * @return the reconciliation response with job ID, metadata, and scored match candidates
     */
    ReconciliationResponse reconcile(ReconciliationRequest request);
}
