package com.prashanthganojiatwork.reconciler.normalizer.strategies;

import com.prashanthganojiatwork.reconciler.model.Address;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Normalizes an {@link Address} into a canonical string form by:
 * <ul>
 *   <li>Concatenating non-null, non-empty structured fields into a single string</li>
 *   <li>Converting to lowercase</li>
 *   <li>Expanding USPS-standard abbreviations to their full forms (word-boundary aware)</li>
 *   <li>Collapsing extra whitespace to a single space</li>
 *   <li>Trimming leading and trailing whitespace</li>
 * </ul>
 *
 * <p>Abbreviation expansion is word-boundary aware, so "dr" inside "andrew" is not expanded,
 * and "street" is not turned into "streetet".</p>
 *
 * <p>Satisfies Requirement 2.4.</p>
 */
public class AddressNormalizationStrategy {

    private static final Map<Pattern, String> ABBREVIATION_EXPANSIONS = new LinkedHashMap<>();

    static {
        // USPS-standard abbreviations mapped to full forms.
        // Using word boundaries (\b) to avoid partial matches within larger words.
        Map<String, String> abbreviations = new LinkedHashMap<>();
        abbreviations.put("st", "street");
        abbreviations.put("ave", "avenue");
        abbreviations.put("blvd", "boulevard");
        abbreviations.put("dr", "drive");
        abbreviations.put("ln", "lane");
        abbreviations.put("rd", "road");
        abbreviations.put("ct", "court");
        abbreviations.put("apt", "apartment");
        abbreviations.put("ste", "suite");
        abbreviations.put("pl", "place");
        abbreviations.put("cir", "circle");
        abbreviations.put("hwy", "highway");
        abbreviations.put("pkwy", "parkway");

        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            // \b ensures we match whole words only
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b");
            ABBREVIATION_EXPANSIONS.put(pattern, entry.getValue());
        }
    }

    /**
     * Normalizes an Address object into a canonical lowercase string with USPS abbreviation expansion.
     *
     * @param address the Address to normalize (may be null)
     * @return the normalized canonical address string, or null if the address is null
     *         or all fields are null/empty
     */
    public String normalize(Address address) {
        if (address == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, address.streetLine1());
        appendIfPresent(sb, address.streetLine2());
        appendIfPresent(sb, address.city());
        appendIfPresent(sb, address.stateCode());
        appendIfPresent(sb, address.postalCode());
        appendIfPresent(sb, address.countryCode());

        if (sb.isEmpty()) {
            return null;
        }

        // Convert to lowercase
        String normalized = sb.toString().toLowerCase();

        // Expand USPS abbreviations (word-boundary aware)
        for (Map.Entry<Pattern, String> entry : ABBREVIATION_EXPANSIONS.entrySet()) {
            normalized = entry.getKey().matcher(normalized).replaceAll(entry.getValue());
        }

        // Collapse extra whitespace to single space and trim
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /**
     * Appends a field value to the builder if it is non-null and non-empty.
     * Adds a space separator before the value if the builder is non-empty.
     */
    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(value);
        }
    }
}
