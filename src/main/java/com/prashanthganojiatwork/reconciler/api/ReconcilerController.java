package com.prashanthganojiatwork.reconciler.api;

import com.prashanthganojiatwork.reconciler.model.ReconciliationRequest;
import com.prashanthganojiatwork.reconciler.model.ReconciliationResponse;
import com.prashanthganojiatwork.reconciler.model.ThresholdConfig;
import com.prashanthganojiatwork.reconciler.orchestrator.ReconciliationOrchestrator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the identity reconciliation API.
 * Handles request validation before delegating to the orchestrator.
 */
@RestController
@RequestMapping("/api/v1")
public class ReconcilerController {

    private static final int MAX_RECORDS_PER_SOURCE = 10_000;

    private final ReconciliationOrchestrator orchestrator;

    public ReconcilerController(ReconciliationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Reconcile two sets of person records.
     *
     * @param request the reconciliation request containing Source A and Source B records
     * @return the reconciliation response with job ID, metadata, and scored matches
     */
    @PostMapping("/reconcile")
    public ReconciliationResponse reconcile(@RequestBody ReconciliationRequest request) {
        validateRequest(request);
        return orchestrator.reconcile(request);
    }

    private void validateRequest(ReconciliationRequest request) {
        // Validate Source A is not null/empty
        if (request.sourceA() == null || request.sourceA().isEmpty()) {
            throw new ValidationException("EMPTY_DATASET", "Source A contains zero records");
        }

        // Validate Source B is not null/empty
        if (request.sourceB() == null || request.sourceB().isEmpty()) {
            throw new ValidationException("EMPTY_DATASET", "Source B contains zero records");
        }

        // Validate record count limits
        if (request.sourceA().size() > MAX_RECORDS_PER_SOURCE) {
            throw new ValidationException("VALIDATION_ERROR",
                "Source A exceeds maximum of " + MAX_RECORDS_PER_SOURCE + " records");
        }
        if (request.sourceB().size() > MAX_RECORDS_PER_SOURCE) {
            throw new ValidationException("VALIDATION_ERROR",
                "Source B exceeds maximum of " + MAX_RECORDS_PER_SOURCE + " records");
        }

        // Validate thresholds if provided
        ThresholdConfig thresholds = request.thresholds();
        if (thresholds != null) {
            if (thresholds.matchThreshold() != null) {
                double mt = thresholds.matchThreshold();
                if (mt < 0.0 || mt > 1.0) {
                    throw new ValidationException("INVALID_THRESHOLD",
                        "matchThreshold must be between 0.0 and 1.0 inclusive, got: " + mt);
                }
            }
            if (thresholds.reviewBandLowerBound() != null) {
                double rb = thresholds.reviewBandLowerBound();
                if (rb < 0.0 || rb > 1.0) {
                    throw new ValidationException("INVALID_THRESHOLD",
                        "reviewBandLowerBound must be between 0.0 and 1.0 inclusive, got: " + rb);
                }
            }
            // Validate reviewBandLowerBound < matchThreshold
            Double mt = thresholds.matchThreshold();
            Double rb = thresholds.reviewBandLowerBound();
            double effectiveMatchThreshold = (mt != null) ? mt : 0.7;
            double effectiveReviewBound = (rb != null) ? rb : 0.4;
            if (effectiveReviewBound >= effectiveMatchThreshold) {
                throw new ValidationException("INVALID_THRESHOLD",
                    "reviewBandLowerBound (" + effectiveReviewBound
                        + ") must be less than matchThreshold (" + effectiveMatchThreshold + ")");
            }
        }
    }
}
