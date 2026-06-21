package com.prashanthganojiatwork.reconciler.normalizer.strategies;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

/**
 * Normalizes date of birth fields by parsing multiple date formats into ISO 8601 ("YYYY-MM-DD").
 * <p>
 * Supported input formats:
 * <ul>
 *   <li>"YYYY-MM-DD" (already ISO)</li>
 *   <li>"MM/DD/YYYY"</li>
 *   <li>"DD-MM-YYYY"</li>
 *   <li>"M/D/YYYY" (single-digit month/day)</li>
 *   <li>"YYYY/MM/DD"</li>
 *   <li>"Month DD, YYYY" (e.g., "January 5, 1990")</li>
 * </ul>
 *
 * <p>If the date cannot be parsed in any format, it is considered unparseable and the
 * original value is returned unchanged (normalization failed).</p>
 *
 * <p>Satisfies Requirements 2.5 and 2.6.</p>
 */
public class DobNormalizationStrategy {

    private static final DateTimeFormatter ISO_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Ordered list of date format patterns to attempt parsing.
     * The order matters: more specific patterns should come before more general ones
     * to avoid ambiguous parsing.
     */
    private static final List<DateTimeFormatter> PARSERS = List.of(
        // "YYYY-MM-DD" (ISO 8601 - already canonical)
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT),

        // "YYYY/MM/DD"
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy/MM/dd")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT),

        // "MM/DD/YYYY" and "M/D/YYYY" (single-digit month/day)
        new DateTimeFormatterBuilder()
            .appendPattern("M/d/yyyy")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT),

        // "DD-MM-YYYY"
        new DateTimeFormatterBuilder()
            .appendPattern("dd-MM-yyyy")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT),

        // "Month DD, YYYY" (e.g., "January 5, 1990")
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy")
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT)
    );

    /**
     * Normalizes a raw date of birth value to ISO 8601 format ("YYYY-MM-DD").
     *
     * @param rawDob the raw date of birth string (may be null or empty)
     * @return the normalized ISO 8601 date string, or null if input is null,
     *         or the original value if unparseable
     */
    public String normalize(String rawDob) {
        if (rawDob == null) {
            return null;
        }

        if (rawDob.isBlank()) {
            return null;
        }

        String trimmed = rawDob.trim();

        for (DateTimeFormatter parser : PARSERS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, parser);
                return date.format(ISO_OUTPUT);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        // None of the formats matched - return original value (unparseable)
        return rawDob;
    }

    /**
     * Parses the raw DOB into a LocalDate if possible.
     *
     * @param rawDob the raw date of birth string
     * @return the parsed LocalDate, or null if unparseable or input is null/blank
     */
    public LocalDate parseToLocalDate(String rawDob) {
        if (rawDob == null || rawDob.isBlank()) {
            return null;
        }

        String trimmed = rawDob.trim();

        for (DateTimeFormatter parser : PARSERS) {
            try {
                return LocalDate.parse(trimmed, parser);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }

    /**
     * Checks whether normalization was successful for the given input.
     * Normalization fails if the date cannot be parsed in any supported format.
     *
     * @param rawDob the raw date of birth string to check
     * @return true if normalization would succeed, false if the value is unparseable
     */
    public boolean isValid(String rawDob) {
        if (rawDob == null || rawDob.isBlank()) {
            return false;
        }
        return parseToLocalDate(rawDob) != null;
    }
}
