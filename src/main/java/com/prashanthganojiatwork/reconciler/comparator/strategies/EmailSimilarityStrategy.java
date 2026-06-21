package com.prashanthganojiatwork.reconciler.comparator.strategies;

import com.prashanthganojiatwork.reconciler.comparator.FieldSimilarityStrategy;

/**
 * Computes similarity between two email address values.
 * <ul>
 *   <li>Exact match on normalized email → 1.0</li>
 *   <li>Levenshtein distance ≤ 2 on local part (typo tolerance) → 0.8</li>
 *   <li>Local part matches (different domain) → 0.7</li>
 *   <li>No match → 0.0</li>
 *   <li>Null values → 0.0</li>
 * </ul>
 *
 * <p>Satisfies Requirements 3.1, 4.3, 10.1.</p>
 */
public class EmailSimilarityStrategy implements FieldSimilarityStrategy {

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
            lastComparisonDescription = "No match (empty email)";
            return 0.0;
        }

        // 1. Exact match on normalized email
        if (normA.equals(normB)) {
            lastComparisonDescription = "Exact match";
            return 1.0;
        }

        // Parse local parts and domains
        String localA = extractLocalPart(normA);
        String localB = extractLocalPart(normB);
        String domainA = extractDomain(normA);
        String domainB = extractDomain(normB);

        if (localA == null || localB == null) {
            lastComparisonDescription = "No match (invalid email format)";
            return 0.0;
        }

        // 2. Levenshtein distance ≤ 2 on local part (typo tolerance) with same domain
        int distance = computeLevenshteinDistance(localA, localB);
        if (distance <= 2 && distance > 0) {
            if (domainA != null && domainA.equals(domainB)) {
                lastComparisonDescription = String.format(
                    "Likely typo in local part (Levenshtein distance: %d, same domain)", distance);
                return 0.8;
            }
            // Different domain but similar local part
            lastComparisonDescription = String.format(
                "Similar local part (Levenshtein distance: %d, different domain)", distance);
            return 0.7;
        }

        // 3. Local part matches exactly (different domain)
        if (localA.equals(localB)) {
            lastComparisonDescription = "Local part matches (different domain: " + domainA + " vs " + domainB + ")";
            return 0.7;
        }

        // 4. No match
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

    private String extractLocalPart(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return null;
        }
        return email.substring(0, atIndex);
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || atIndex >= email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     */
    static int computeLevenshteinDistance(String a, String b) {
        int lenA = a.length();
        int lenB = b.length();

        int[][] dp = new int[lenA + 1][lenB + 1];

        for (int i = 0; i <= lenA; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= lenB; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[lenA][lenB];
    }
}
