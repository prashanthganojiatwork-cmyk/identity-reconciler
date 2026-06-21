package com.prashanthganojiatwork.reconciler.normalizer.strategies;

/**
 * Normalizes phone number fields by:
 * <ul>
 *   <li>Stripping all non-digit characters (parentheses, dashes, spaces, dots, plus signs)</li>
 *   <li>Prepending country code "1" if the resulting digit string is 10 digits</li>
 *   <li>Returning the digit-only representation with country code prefix</li>
 * </ul>
 *
 * <p>If the phone has fewer than 7 digits after stripping, it is considered unparseable
 * and the original value is returned unchanged (normalization failed).</p>
 *
 * <p>Satisfies Requirements 2.2 and 2.6.</p>
 */
public class PhoneNormalizationStrategy {

    /**
     * Normalizes a raw phone number value.
     *
     * @param rawPhone the raw phone string (may be null or empty)
     * @return the normalized digit-only phone with country code, or null if input is null,
     *         or the original value if unparseable (fewer than 7 digits)
     */
    public String normalize(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        if (rawPhone.isBlank()) {
            return null;
        }

        // Strip all non-digit characters
        String digitsOnly = rawPhone.replaceAll("[^0-9]", "");

        // If fewer than 7 digits, it is unparseable - return original value
        if (digitsOnly.length() < 7) {
            return rawPhone;
        }

        // If exactly 10 digits, prepend country code "1"
        if (digitsOnly.length() == 10) {
            digitsOnly = "1" + digitsOnly;
        }

        return digitsOnly;
    }

    /**
     * Checks whether normalization was successful for the given input.
     * Normalization fails if the input has fewer than 7 digits after stripping non-digit characters.
     *
     * @param rawPhone the raw phone string to check
     * @return true if normalization would succeed, false if the value is unparseable
     */
    public boolean isValid(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return false;
        }

        String digitsOnly = rawPhone.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 7;
    }
}
