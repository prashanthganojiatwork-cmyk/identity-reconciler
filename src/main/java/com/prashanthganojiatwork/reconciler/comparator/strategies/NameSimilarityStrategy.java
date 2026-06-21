package com.prashanthganojiatwork.reconciler.comparator.strategies;

import com.prashanthganojiatwork.reconciler.comparator.FieldSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.NameNormalizationStrategy;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Computes similarity between two name values using multiple comparison techniques
 * ordered by match strength:
 * <ol>
 *   <li>Exact match after normalization → 1.0</li>
 *   <li>Nickname resolution (Bill↔William) → 0.95</li>
 *   <li>Phonetic match (Soundex) → 0.85</li>
 *   <li>Jaro-Winkler similarity ≥ 0.85 → JW score</li>
 *   <li>Swapped first/last name detection → 0.90</li>
 *   <li>Initial match (J Smith = John Smith) → 0.80</li>
 *   <li>Partial match (one name component matches) → 0.5-0.7</li>
 *   <li>No match → 0.0</li>
 * </ol>
 *
 * <p>Satisfies Requirements 3.1, 4.3, 10.1.</p>
 */
public class NameSimilarityStrategy implements FieldSimilarityStrategy {

    private final NameNormalizationStrategy nameNormalization;
    private final JaroWinklerSimilarity jaroWinkler;

    private String lastComparisonDescription;

    public NameSimilarityStrategy() {
        this.nameNormalization = new NameNormalizationStrategy();
        this.jaroWinkler = new JaroWinklerSimilarity();
    }

    public NameSimilarityStrategy(NameNormalizationStrategy nameNormalization) {
        this.nameNormalization = nameNormalization;
        this.jaroWinkler = new JaroWinklerSimilarity();
    }

    @Override
    public double computeSimilarity(String valueA, String valueB) {
        if (valueA == null || valueB == null) {
            lastComparisonDescription = "No match (null value)";
            return 0.0;
        }

        String normA = valueA.toLowerCase().trim();
        String normB = valueB.toLowerCase().trim();

        if (normA.isEmpty() || normB.isEmpty()) {
            lastComparisonDescription = "No match (empty value)";
            return 0.0;
        }

        // 1. Exact match after normalization
        if (normA.equals(normB)) {
            lastComparisonDescription = "Exact match";
            return 1.0;
        }

        // 2. Nickname resolution
        String canonicalA = nameNormalization.getCanonicalName(normA);
        String canonicalB = nameNormalization.getCanonicalName(normB);
        if (canonicalA != null && canonicalB != null && canonicalA.equals(canonicalB)) {
            lastComparisonDescription = "Matched via nickname resolution: " + normA + " \u2194 " + normB;
            return 0.95;
        }

        // 3. Phonetic match (Soundex)
        String soundexA = computeSoundex(normA);
        String soundexB = computeSoundex(normB);
        if (soundexA != null && soundexB != null && soundexA.equals(soundexB)) {
            lastComparisonDescription = "Phonetic match (Soundex): " + normA + " \u2194 " + normB;
            return 0.85;
        }

        // 4. Jaro-Winkler similarity
        double jwScore = jaroWinkler.apply(normA, normB);
        if (jwScore >= 0.85) {
            lastComparisonDescription = String.format("Jaro-Winkler similarity: %.2f", jwScore);
            return jwScore;
        }

        // 5. Swapped first/last name detection
        String[] partsA = normA.split("\\s+");
        String[] partsB = normB.split("\\s+");
        if (partsA.length >= 2 && partsB.length >= 2) {
            Set<String> setA = new HashSet<>(Arrays.asList(partsA));
            Set<String> setB = new HashSet<>(Arrays.asList(partsB));
            if (setA.equals(setB) && !normA.equals(normB)) {
                lastComparisonDescription = "Swapped name components detected: " + normA + " \u2194 " + normB;
                return 0.90;
            }
        }

        // 6. Initial match (J Smith = John Smith)
        if (isInitialMatch(partsA, partsB)) {
            lastComparisonDescription = "Initial match: " + normA + " \u2194 " + normB;
            return 0.80;
        }

        // 7. Partial match (one name component matches)
        double partialScore = computePartialMatch(partsA, partsB);
        if (partialScore > 0.0) {
            lastComparisonDescription = String.format("Partial match (shared component): %.1f", partialScore);
            return partialScore;
        }

        // 8. No match
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
     * Checks if one name uses initials that match the other name's full components.
     */
    private boolean isInitialMatch(String[] partsA, String[] partsB) {
        if (partsA.length < 2 || partsB.length < 2) {
            return false;
        }
        return isInitialOf(partsA, partsB) || isInitialOf(partsB, partsA);
    }

    private boolean isInitialOf(String[] shorter, String[] longer) {
        for (int i = 0; i < shorter.length; i++) {
            if (shorter[i].length() == 1 && i < longer.length) {
                if (longer[i].length() > 1 && longer[i].charAt(0) == shorter[i].charAt(0)) {
                    boolean otherMatch = false;
                    for (int j = 0; j < shorter.length; j++) {
                        if (j != i && j < longer.length && shorter[j].equals(longer[j])) {
                            otherMatch = true;
                            break;
                        }
                    }
                    if (otherMatch) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Computes partial match score when at least one component matches exactly.
     */
    private double computePartialMatch(String[] partsA, String[] partsB) {
        if (partsA.length == 0 || partsB.length == 0) {
            return 0.0;
        }

        Set<String> setA = new HashSet<>(Arrays.asList(partsA));
        Set<String> setB = new HashSet<>(Arrays.asList(partsB));

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        if (intersection.isEmpty()) {
            return 0.0;
        }

        // If last component (likely surname) matches
        String lastA = partsA[partsA.length - 1];
        String lastB = partsB[partsB.length - 1];
        if (lastA.equals(lastB)) {
            return 0.7;
        }

        // If first component matches (given name)
        if (partsA[0].equals(partsB[0])) {
            return 0.6;
        }

        // Any other component matches
        return 0.5;
    }

    /**
     * Computes a Soundex code for phonetic matching.
     */
    static String computeSoundex(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        String cleaned = name.replaceAll("[^a-zA-Z]", "");
        if (cleaned.isEmpty()) {
            return null;
        }

        char[] result = new char[4];
        result[0] = Character.toUpperCase(cleaned.charAt(0));

        int index = 1;
        char lastCode = soundexCode(cleaned.charAt(0));

        for (int i = 1; i < cleaned.length() && index < 4; i++) {
            char code = soundexCode(cleaned.charAt(i));
            if (code != '0' && code != lastCode) {
                result[index++] = code;
            }
            lastCode = code;
        }

        while (index < 4) {
            result[index++] = '0';
        }

        return new String(result);
    }

    private static char soundexCode(char c) {
        c = Character.toUpperCase(c);
        switch (c) {
            case 'B': case 'F': case 'P': case 'V':
                return '1';
            case 'C': case 'G': case 'J': case 'K': case 'Q': case 'S': case 'X': case 'Z':
                return '2';
            case 'D': case 'T':
                return '3';
            case 'L':
                return '4';
            case 'M': case 'N':
                return '5';
            case 'R':
                return '6';
            default:
                return '0';
        }
    }
}
