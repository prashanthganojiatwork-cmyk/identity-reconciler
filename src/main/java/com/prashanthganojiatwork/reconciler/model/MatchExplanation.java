package com.prashanthganojiatwork.reconciler.model;

import java.util.List;

/**
 * Human-readable explanation of a match result, including per-field breakdown,
 * overall score, and ambiguity information.
 *
 * @param summary           human-readable summary of the match (max 500 characters)
 * @param fieldBreakdowns   per-field detailed breakdown showing how each field contributed
 * @param totalScore        final confidence score (should equal sum of field contributions)
 * @param ambiguous         true if the match is in the review band with conflicting fields
 * @param ambiguityReasons  specific reasons for ambiguity (empty list if not ambiguous)
 */
public record MatchExplanation(
    String summary,
    List<FieldBreakdown> fieldBreakdowns,
    double totalScore,
    boolean ambiguous,
    List<String> ambiguityReasons
) {}
