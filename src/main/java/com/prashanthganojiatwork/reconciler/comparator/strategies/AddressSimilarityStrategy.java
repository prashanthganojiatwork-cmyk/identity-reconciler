package com.prashanthganojiatwork.reconciler.comparator.strategies;

import com.prashanthganojiatwork.reconciler.comparator.FieldSimilarityStrategy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes similarity between two address values using token-based Jaccard similarity.
 * <ul>
 *   <li>Exact match on normalized address string → 1.0</li>
 *   <li>Token-based Jaccard similarity (split on spaces, compute intersection/union)</li>
 *   <li>If Jaccard ≥ 0.5, return it; otherwise 0.0</li>
 *   <li>Null values → 0.0</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.1, 4.3, 10.1.</p>
 */
public class AddressSimilarityStrategy implements FieldSimilarityStrategy {

    private String lastComparisonDescription;

    @Override
    public double computeSimilarity(String valueA, String valueB) {
        if (valueA == null || valueB == null) {
            lastComparisonDescription = "No match (null value)";
            return 0.0;
        }

        String normA = valueA.toLowerCase().trim();
        String normB = valueB.toLowerCase().trim();

        if (normA.isEmpty() || normB.isEmpty()) {
            lastComparisonDescription = "No match (empty address)";
            return 0.0;
        }

        // 1. Exact match on normalized address string
        if (normA.equals(normB)) {
            lastComparisonDescription = "Exact match";
            return 1.0;
        }

        // 2. Token-based Jaccard similarity
        Set<String> tokensA = tokenize(normA);
        Set<String> tokensB = tokenize(normB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            lastComparisonDescription = "No match (no tokens)";
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        double jaccard = (double) intersection.size() / union.size();

        // 3. If Jaccard >= 0.5, return it; otherwise 0.0
        if (jaccard >= 0.5) {
            lastComparisonDescription = String.format("Jaccard similarity: %.2f (%d/%d tokens overlap)",
                jaccard, intersection.size(), union.size());
            return jaccard;
        }

        lastComparisonDescription = String.format("Low Jaccard similarity: %.2f (below threshold)", jaccard);
        return 0.0;
    }

    @Override
    public String describeComparison(String valueA, String valueB) {
        if (lastComparisonDescription == null) {
            computeSimilarity(valueA, valueB);
        }
        String description = lastComparisonDescription;
        lastComparisonDescription = null;
        return description;
    }

    /**
     * Tokenizes an address string by splitting on whitespace and filtering out
     * empty tokens. Tokens are already lowercased from normalization.
     */
    private Set<String> tokenize(String address) {
        return Arrays.stream(address.split("\\s+"))
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toSet());
    }
}
