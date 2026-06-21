package com.prashanthganojiatwork.reconciler.normalizer.strategies;

/**
 * Normalizes email fields by:
 * <ul>
 *   <li>Converting the entire email to lowercase</li>
 *   <li>Removing the "+" character and all subsequent characters before the "@" symbol (plus-addressing)</li>
 * </ul>
 *
 * <p>If the email is missing "@", it is considered unparseable and the original value
 * is returned unchanged (normalization failed).</p>
 *
 * <p>Satisfies Requirements 2.3 and 2.6.</p>
 */
public class EmailNormalizationStrategy {

    /**
     * Normalizes a raw email value.
     *
     * @param rawEmail the raw email string (may be null or empty)
     * @return the normalized email (lowercase, plus-addressing removed), or null if input is null,
     *         or the original value if unparseable (missing "@")
     */
    public String normalize(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }

        if (rawEmail.isBlank()) {
            return null;
        }

        // Check for "@" - if missing, it is unparseable
        int atIndex = rawEmail.indexOf('@');
        if (atIndex < 0) {
            return rawEmail;
        }

        // Convert to lowercase
        String email = rawEmail.toLowerCase();

        // Recalculate atIndex after lowercasing (position unchanged, but good practice)
        atIndex = email.indexOf('@');

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex); // includes the "@"

        // Remove plus-addressing: strip "+" and everything after it in the local part
        int plusIndex = localPart.indexOf('+');
        if (plusIndex >= 0) {
            localPart = localPart.substring(0, plusIndex);
        }

        return localPart + domainPart;
    }

    /**
     * Checks whether normalization was successful for the given input.
     * Normalization fails if the email is missing the "@" symbol.
     *
     * @param rawEmail the raw email string to check
     * @return true if normalization would succeed, false if the value is unparseable
     */
    public boolean isValid(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return false;
        }

        return rawEmail.contains("@");
    }
}
