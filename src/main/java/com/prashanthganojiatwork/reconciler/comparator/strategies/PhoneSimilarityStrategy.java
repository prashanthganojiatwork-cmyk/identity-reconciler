package com.prashanthganojiatwork.reconciler.comparator.strategies;

import com.prashanthganojiatwork.reconciler.comparator.FieldSimilarityStrategy;

/**
 * Computes similarity between two phone number values.
 * <ul>
 *   <li>Exact match on normalized digit strings → 1.0</li>
 *   <li>Last 7 digits match (handles different country code formats) → 0.8</li>
 *   <li>No match → 0.0</li>
 *   <li>Null values → 0.0</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.1, 4.3, 10.1.</p>
 */
public class PhoneSimilarityStrategy implements FieldSimilarityStrategy {

    private String lastComparisonDescription;

    @Override
    public double computeSimilarity(String valueA, String valueB) {
        if (valueA == null || valueB == null) {
            lastComparisonDescription = "No match (null value)";
            return 0.0;
        }

        String digitsA = valueA.replaceAll("[^0-9]", "");
        String digitsB = valueB.replaceAll("[^0-9]", "");

        if (digitsA.isEmpty() || digitsB.isEmpty()) {
            lastComparisonDescription = "No match (empty phone number)";
            return 0.0;
        }

        // 1. Exact match on normalized digit strings
        if (digitsA.equals(digitsB)) {
            lastComparisonDescription = "Exact match on normalized digits";
            return 1.0;
        }

        // 2. Last 7 digits match (handles different country code formats)
        if (digitsA.length() >= 7 && digitsB.length() >= 7) {
            String lastSevenA = digitsA.substring(digitsA.length() - 7);
            String lastSevenB = digitsB.substring(digitsB.length() - 7);
            if (lastSevenA.equals(lastSevenB)) {
                lastComparisonDescription = "Last 7 digits match (different country code/area code format)";
                return 0.8;
            }
        }

        // 3. No match
        lastComparisonDescription = "No match";
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
}
