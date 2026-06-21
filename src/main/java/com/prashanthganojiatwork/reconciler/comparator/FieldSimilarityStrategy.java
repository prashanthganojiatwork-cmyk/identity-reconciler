package com.prashanthganojiatwork.reconciler.comparator;

/**
 * Strategy interface for computing similarity between two field values.
 * Each field type (name, phone, email, address, DOB) has its own strategy implementation
 * with appropriate algorithms (Jaro-Winkler, exact match, Jaccard, etc.).
 *
 * <p>This interface is transport-agnostic: it uses no Spring annotations, HTTP types,
 * or in-memory collection internals, ensuring the contract can be used across
 * different deployment topologies (Req 10.5).</p>
 */
public interface FieldSimilarityStrategy {

    /**
     * Computes the similarity between two field values.
     *
     * @param valueA the normalized value from Source A
     * @param valueB the normalized value from Source B
     * @return a similarity score between 0.0 (completely different) and 1.0 (identical)
     */
    double computeSimilarity(String valueA, String valueB);

    /**
     * Produces a human-readable description of what comparison logic was applied
     * and the outcome. Used by the Explanation Builder for match explanations.
     *
     * @param valueA the normalized value from Source A
     * @param valueB the normalized value from Source B
     * @return a description of the comparison (e.g., "Names matched via nickname resolution: bill → william")
     */
    String describeComparison(String valueA, String valueB);
}
