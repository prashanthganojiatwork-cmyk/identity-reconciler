package com.prashanthganojiatwork.reconciler.comparator.strategies;

import com.prashanthganojiatwork.reconciler.comparator.FieldSimilarityStrategy;

/**
 * Computes similarity between two date-of-birth values.
 * Expects values in normalized ISO 8601 format (YYYY-MM-DD) from the normalizer.
 * <ul>
 *   <li>Exact match on normalized date → 1.0</li>
 *   <li>2-of-3 components match (year+month, year+day, or month+day) → 0.7</li>
 *   <li>1-of-3 matches (only year) → 0.3</li>
 *   <li>No match → 0.0</li>
 *   <li>Null values → 0.0</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.1, 4.3, 10.1.</p>
 */
public class DobSimilarityStrategy implements FieldSimilarityStrategy {

    private String lastComparisonDescription;

    @Override
    public double computeSimilarity(String valueA, String valueB) {
        if (valueA == null || valueB == null) {
            lastComparisonDescription = "No match (null value)";
            return 0.0;
        }

        String normA = valueA.trim();
        String normB = valueB.trim();

        if (normA.isEmpty() || normB.isEmpty()) {
            lastComparisonDescription = "No match (empty date)";
            return 0.0;
        }

        // 1. Exact match
        if (normA.equals(normB)) {
            lastComparisonDescription = "Exact match";
            return 1.0;
        }

        // Parse components (expecting YYYY-MM-DD format from normalizer)
        int[] componentsA = parseDateComponents(normA);
        int[] componentsB = parseDateComponents(normB);

        if (componentsA == null || componentsB == null) {
            lastComparisonDescription = "No match (unparseable date format)";
            return 0.0;
        }

        int yearA = componentsA[0], monthA = componentsA[1], dayA = componentsA[2];
        int yearB = componentsB[0], monthB = componentsB[1], dayB = componentsB[2];

        boolean yearMatch = (yearA == yearB);
        boolean monthMatch = (monthA == monthB);
        boolean dayMatch = (dayA == dayB);

        int matchCount = (yearMatch ? 1 : 0) + (monthMatch ? 1 : 0) + (dayMatch ? 1 : 0);

        // 2. 2-of-3 components match (handles transposition errors)
        if (matchCount == 2) {
            String matchedParts = describeMatchedComponents(yearMatch, monthMatch, dayMatch);
            lastComparisonDescription = "Partial match (2 of 3 components: " + matchedParts + ")";
            return 0.7;
        }

        // 3. 1-of-3 matches (only year)
        if (matchCount == 1 && yearMatch) {
            lastComparisonDescription = "Partial match (year only: " + yearA + ")";
            return 0.3;
        }

        // 4. No meaningful match
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

    /**
     * Parses a date string into [year, month, day] components.
     * Supports YYYY-MM-DD format (normalized output from DobNormalizationStrategy).
     * Also handles LocalDate.toString() format.
     *
     * @return int array [year, month, day] or null if unparseable
     */
    private int[] parseDateComponents(String dateStr) {
        try {
            // Try YYYY-MM-DD format
            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (year > 0 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return new int[]{year, month, day};
                }
            }
        } catch (NumberFormatException e) {
            // Fall through to return null
        }
        return null;
    }

    private String describeMatchedComponents(boolean yearMatch, boolean monthMatch, boolean dayMatch) {
        StringBuilder sb = new StringBuilder();
        if (yearMatch) sb.append("year");
        if (monthMatch) {
            if (sb.length() > 0) sb.append("+");
            sb.append("month");
        }
        if (dayMatch) {
            if (sb.length() > 0) sb.append("+");
            sb.append("day");
        }
        return sb.toString();
    }
}
