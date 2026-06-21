package com.prashanthganojiatwork.reconciler.normalizer.strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes name fields by applying:
 * <ul>
 *   <li>Lowercase conversion</li>
 *   <li>Whitespace collapse (consecutive whitespace → single space)</li>
 *   <li>Trim leading/trailing whitespace</li>
 *   <li>Retain hyphens and apostrophes (meaningful characters)</li>
 *   <li>Remove periods (e.g., "Jr." → "Jr")</li>
 *   <li>Nickname resolution via configurable dictionary</li>
 * </ul>
 *
 * <p>Satisfies Requirements 2.1 and 2.7.</p>
 */
public class NameNormalizationStrategy {

    private final Map<String, String> nicknameDictionary;

    /**
     * Constructs the strategy, loading the nickname dictionary from the classpath resource.
     */
    public NameNormalizationStrategy() {
        this.nicknameDictionary = loadNicknameDictionary("nicknames.json");
    }

    /**
     * Constructs the strategy with a custom nickname dictionary.
     *
     * @param nicknameDictionary a map from lowercase nickname/canonical to the canonical form
     */
    public NameNormalizationStrategy(Map<String, String> nicknameDictionary) {
        this.nicknameDictionary = new HashMap<>(nicknameDictionary);
    }

    /**
     * Normalizes a raw name value.
     *
     * @param rawName the raw name string (may be null or empty)
     * @return the normalized name, or null if input is null, or empty string if input is blank
     */
    public String normalize(String rawName) {
        if (rawName == null) {
            return null;
        }

        if (rawName.isBlank()) {
            return "";
        }

        // 1. Lowercase
        String result = rawName.toLowerCase();

        // 2. Remove periods (e.g., "Jr." → "Jr")
        result = result.replace(".", "");

        // 3. Collapse consecutive whitespace to a single space
        result = result.replaceAll("\\s+", " ");

        // 4. Trim leading and trailing whitespace
        result = result.trim();

        // 5. Resolve nicknames — apply to each word token individually
        result = resolveNicknames(result);

        return result;
    }

    /**
     * Resolves nicknames within the name by replacing each token with its canonical form
     * if found in the dictionary. Hyphens and apostrophes are preserved.
     */
    private String resolveNicknames(String normalizedName) {
        // Split on spaces to process each name part independently
        String[] tokens = normalizedName.split(" ");
        StringBuilder resolved = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                resolved.append(" ");
            }
            String token = tokens[i];
            // Look up the token in the nickname dictionary
            String canonical = nicknameDictionary.get(token);
            resolved.append(canonical != null ? canonical : token);
        }

        return resolved.toString();
    }

    /**
     * Loads the nickname dictionary from a JSON resource file on the classpath.
     * The dictionary is bidirectional: each nickname maps to the canonical (longest/most formal) form,
     * and the canonical name also maps to itself for consistency.
     *
     * @param resourcePath the classpath resource path (e.g., "nicknames.json")
     * @return a map from lowercase name variant to lowercase canonical name
     */
    private static Map<String, String> loadNicknameDictionary(String resourcePath) {
        Map<String, String> dictionary = new HashMap<>();

        try (InputStream is = NameNormalizationStrategy.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Nickname dictionary resource not found on classpath: " + resourcePath);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode mappings = root.get("mappings");

            if (mappings != null && mappings.isArray()) {
                for (JsonNode entry : mappings) {
                    String canonical = entry.get("canonical").asText().toLowerCase();
                    // Map canonical to itself
                    dictionary.put(canonical, canonical);

                    JsonNode nicknames = entry.get("nicknames");
                    if (nicknames != null && nicknames.isArray()) {
                        for (JsonNode nick : nicknames) {
                            dictionary.put(nick.asText().toLowerCase(), canonical);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load nickname dictionary: " + resourcePath, e);
        }

        return dictionary;
    }

    /**
     * Returns the canonical form of a name if it exists in the nickname dictionary.
     *
     * @param name the name to look up (case-insensitive)
     * @return the canonical form if found, or null if not in the dictionary
     */
    public String getCanonicalName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return nicknameDictionary.get(name.toLowerCase().trim());
    }
}
