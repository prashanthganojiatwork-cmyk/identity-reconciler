package com.prashanthganojiatwork.reconciler.normalizer.property;

import com.prashanthganojiatwork.reconciler.model.Address;
import com.prashanthganojiatwork.reconciler.model.NormalizationResult;
import com.prashanthganojiatwork.reconciler.model.NormalizationWarning;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import com.prashanthganojiatwork.reconciler.model.PersonRecord;
import com.prashanthganojiatwork.reconciler.normalizer.DefaultNormalizer;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.AddressNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.DobNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.EmailNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.NameNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.PhoneNormalizationStrategy;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Property-based tests for the Normalizer component using jqwik.
 *
 * <p>Property 1: Normalization Idempotence — normalize(normalize(x)) == normalize(x)</p>
 * <p>Property 2: Invalid Input Passthrough — unparseable values returned unchanged with warning</p>
 */
class NormalizerPropertyTest {

    private final DefaultNormalizer normalizer = new DefaultNormalizer(
            new NameNormalizationStrategy(),
            new PhoneNormalizationStrategy(),
            new EmailNormalizationStrategy(),
            new DobNormalizationStrategy(),
            new AddressNormalizationStrategy()
    );

    // =========================================================================
    // Property 1: Normalization Idempotence
    // =========================================================================

    /**
     * Property 1: Normalization Idempotence
     *
     * For any valid PersonRecord, normalizing it once and normalizing the result a second time
     * SHALL produce the same output: normalize(normalize(x)) == normalize(x)
     *
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.7
     */
    @Property(tries = 200)
    @Tag("Feature: identity-reconciler, Property 1: Normalization Idempotence")
    void normalizationIsIdempotent(@ForAll("validPersonRecords") PersonRecord record) {
        // First normalization
        NormalizationResult firstResult = normalizer.normalize(record);
        NormalizedRecord firstNormalized = firstResult.normalizedRecord();

        // Build a PersonRecord from the normalized values to normalize again
        PersonRecord fromNormalized = buildPersonRecordFromNormalized(firstNormalized);

        // Second normalization
        NormalizationResult secondResult = normalizer.normalize(fromNormalized);
        NormalizedRecord secondNormalized = secondResult.normalizedRecord();

        // Compare all normalized fields — they should be identical
        assertNormalizedRecordsEqual(firstNormalized, secondNormalized);
    }

    // =========================================================================
    // Property 2: Invalid Input Passthrough
    // =========================================================================

    /**
     * Property 2: Invalid Input Passthrough — malformed phone numbers
     *
     * For any phone value with fewer than 7 digits, the Normalizer SHALL return the original
     * value unchanged AND produce a non-empty normalization warning.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 200)
    @Tag("Feature: identity-reconciler, Property 2: Invalid Input Passthrough")
    void invalidPhonePassesThroughWithWarning(@ForAll("malformedPhones") String phone) {
        PersonRecord record = new PersonRecord(
                UUID.randomUUID().toString(),
                null, null, null, null, null,
                phone,
                null, null
        );

        NormalizationResult result = normalizer.normalize(record);

        // The normalized phone should be the original value (unchanged)
        assert Objects.equals(result.normalizedRecord().normalizedPhone(), phone)
                : "Expected original phone '" + phone + "' but got '"
                + result.normalizedRecord().normalizedPhone() + "'";

        // Should have a warning for phone
        assert result.warnings().stream()
                .anyMatch(w -> w.fieldName().equals("phone"))
                : "Expected a normalization warning for phone field, but got none. Warnings: "
                + result.warnings();
    }

    /**
     * Property 2: Invalid Input Passthrough — malformed emails
     *
     * For any email value missing "@", the Normalizer SHALL return the original value unchanged
     * AND produce a non-empty normalization warning.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 200)
    @Tag("Feature: identity-reconciler, Property 2: Invalid Input Passthrough")
    void invalidEmailPassesThroughWithWarning(@ForAll("malformedEmails") String email) {
        PersonRecord record = new PersonRecord(
                UUID.randomUUID().toString(),
                null, null, null, null, null, null,
                email,
                null
        );

        NormalizationResult result = normalizer.normalize(record);

        // The normalized email should be the original value (unchanged)
        assert Objects.equals(result.normalizedRecord().normalizedEmail(), email)
                : "Expected original email '" + email + "' but got '"
                + result.normalizedRecord().normalizedEmail() + "'";

        // Should have a warning for email
        assert result.warnings().stream()
                .anyMatch(w -> w.fieldName().equals("email"))
                : "Expected a normalization warning for email field, but got none. Warnings: "
                + result.warnings();
    }

    /**
     * Property 2: Invalid Input Passthrough — malformed dates
     *
     * For any date value that cannot be parsed (e.g., "13/45/2000"), the Normalizer SHALL
     * return null for the DOB field AND produce a non-empty normalization warning.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 200)
    @Tag("Feature: identity-reconciler, Property 2: Invalid Input Passthrough")
    void invalidDobPassesThroughWithWarning(@ForAll("malformedDates") String dob) {
        PersonRecord record = new PersonRecord(
                UUID.randomUUID().toString(),
                null, null, null, null,
                dob,
                null, null, null
        );

        NormalizationResult result = normalizer.normalize(record);

        // The normalized DOB should be null (unparseable)
        assert result.normalizedRecord().normalizedDob() == null
                : "Expected null DOB for unparseable date '" + dob + "' but got '"
                + result.normalizedRecord().normalizedDob() + "'";

        // Should have a warning for dateOfBirth
        assert result.warnings().stream()
                .anyMatch(w -> w.fieldName().equals("dateOfBirth"))
                : "Expected a normalization warning for dateOfBirth field, but got none. Warnings: "
                + result.warnings();
    }

    // =========================================================================
    // Generators (Arbitraries)
    // =========================================================================

    /**
     * Generates valid PersonRecords with realistic field distributions.
     * Fields are randomly null or populated with various formatting.
     */
    @Provide
    Arbitrary<PersonRecord> validPersonRecords() {
        Arbitrary<String> ids = Arbitraries.strings().alpha().ofLength(8);
        Arbitrary<String> names = validNames();
        Arbitrary<String> phones = validPhones();
        Arbitrary<String> emails = validEmails();
        Arbitrary<String> dobs = validDobs();
        Arbitrary<Address> addresses = validAddresses();

        return Combinators.combine(ids, names, names, names, phones, emails, dobs, addresses)
                .as((id, first, middle, last, phone, email, dob, address) ->
                        new PersonRecord(
                                id,
                                nullableWith(first, 0.15),
                                nullableWith(middle, 0.4),
                                nullableWith(last, 0.15),
                                null, // alternateNames (kept simple)
                                nullableWith(dob, 0.2),
                                nullableWith(phone, 0.2),
                                nullableWith(email, 0.2),
                                nullableWith(address, 0.3)
                        )
                );
    }

    /**
     * Generates malformed phone numbers with 1-6 digits.
     */
    @Provide
    Arbitrary<String> malformedPhones() {
        return Arbitraries.of(
                // 1-6 digit strings, possibly with formatting
                "1",
                "12",
                "123",
                "1234",
                "12345",
                "123456",
                "(12) 3",
                "45-67",
                "abc12",
                "+1-23",
                "5",
                "98"
        ).flatMap(base ->
                Arbitraries.of("", "-", " ", "(", ")", ".", "+")
                        .list().ofMinSize(0).ofMaxSize(2)
                        .map(separators -> {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < base.length(); i++) {
                                if (i > 0 && !separators.isEmpty()) {
                                    sb.append(separators.get(i % separators.size()));
                                }
                                sb.append(base.charAt(i));
                            }
                            return sb.toString();
                        })
        ).filter(phone -> {
            // Ensure < 7 digits after stripping non-digits
            String digits = phone.replaceAll("[^0-9]", "");
            return digits.length() >= 1 && digits.length() < 7;
        });
    }

    /**
     * Generates malformed emails missing the "@" symbol.
     */
    @Provide
    Arbitrary<String> malformedEmails() {
        return Arbitraries.of(
                "userexample.com",
                "john.doe.gmail.com",
                "noatsign",
                "missing-at-sign.org",
                "hello world",
                "test!email.com",
                "justtext",
                "user.name.domain",
                "no_at_here",
                "invalidemailformat"
        );
    }

    /**
     * Generates invalid/unparseable date strings.
     */
    @Provide
    Arbitrary<String> malformedDates() {
        return Arbitraries.of(
                "13/45/2000",
                "00/00/0000",
                "32/01/1990",
                "not-a-date",
                "2000-13-01",
                "2000-00-15",
                "abc",
                "99/99/9999",
                "Smarch 5, 1990",
                "Januray 40, 2020",
                "12-32-1985",
                "0000-00-00",
                "2023-02-30",
                "hello world"
        );
    }

    // =========================================================================
    // Helper generators
    // =========================================================================

    private Arbitrary<String> validNames() {
        return Arbitraries.of(
                "John", "Jane", "Bill", "Bob", "William", "Robert",
                "Mary-Jane", "O'Brien", "De La Cruz", "Smith Jr",
                "ALICE", "  bob  ", "Jim.Jr", "Dr. Smith",
                "Katherine", "mike", "Jenny", "THOMAS"
        );
    }

    private Arbitrary<String> validPhones() {
        return Arbitraries.of(
                "2065551234",
                "(206) 555-1234",
                "206.555.1234",
                "+1-206-555-1234",
                "1-800-555-0100",
                "5551234567",
                "+12065551234",
                "206-555-1234"
        );
    }

    private Arbitrary<String> validEmails() {
        return Arbitraries.of(
                "john@example.com",
                "Jane.Doe@Gmail.Com",
                "user+tag@example.com",
                "USER@DOMAIN.ORG",
                "alice+filter@mail.co",
                "test.email@sub.domain.com"
        );
    }

    private Arbitrary<String> validDobs() {
        return Arbitraries.of(
                "1990-05-15",
                "05/15/1990",
                "15-05-1990",
                "5/5/1990",
                "1990/05/15",
                "January 5, 1990",
                "2000-12-31",
                "12/31/2000",
                "1985-01-01"
        );
    }

    private Arbitrary<Address> validAddresses() {
        Arbitrary<String> streets = Arbitraries.of(
                "123 Main St", "456 Oak Ave", "789 Elm Blvd",
                "100 Park Dr", "55 Cedar Ln", null
        );
        Arbitrary<String> cities = Arbitraries.of(
                "Seattle", "Portland", "San Francisco", null
        );
        Arbitrary<String> states = Arbitraries.of(
                "WA", "OR", "CA", null
        );
        Arbitrary<String> zips = Arbitraries.of(
                "98101", "97201", "94105", null
        );

        return Combinators.combine(streets, cities, states, zips)
                .as((street, city, state, zip) ->
                        new Address(street, null, city, state, zip, "US")
                );
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Randomly returns null with the given probability to simulate missing fields.
     * Uses deterministic hash-based approach for jqwik reproducibility.
     */
    private static <T> T nullableWith(T value, double nullProbability) {
        if (value == null) return null;
        // Use hashCode for deterministic behavior under jqwik shrinking
        int hash = System.identityHashCode(value);
        return (Math.abs(hash) % 100) < (nullProbability * 100) ? null : value;
    }

    /**
     * Builds a PersonRecord from a NormalizedRecord to enable testing idempotence.
     * The normalized values become the "raw" input for the second normalization pass.
     */
    private PersonRecord buildPersonRecordFromNormalized(NormalizedRecord normalized) {
        // Convert normalizedDob back to string (ISO format, which is what the normalizer outputs)
        String dobString = normalized.normalizedDob() != null
                ? normalized.normalizedDob().toString()
                : null;

        // Build an Address from the normalized address string (simplified: put it in streetLine1)
        Address address = null;
        if (normalized.normalizedAddress() != null) {
            address = new Address(normalized.normalizedAddress(), null, null, null, null, null);
        }

        return new PersonRecord(
                normalized.id(),
                normalized.normalizedFirstName(),
                null, // middlename not tracked in NormalizedRecord
                normalized.normalizedLastName(),
                normalized.alternateNames().isEmpty() ? null : normalized.alternateNames(),
                dobString,
                normalized.normalizedPhone(),
                normalized.normalizedEmail(),
                address
        );
    }

    /**
     * Asserts that two NormalizedRecords have equal normalized field values.
     * Compares all normalized fields individually for clear failure messages.
     */
    private void assertNormalizedRecordsEqual(NormalizedRecord first, NormalizedRecord second) {
        assert Objects.equals(first.normalizedFirstName(), second.normalizedFirstName())
                : "firstName mismatch: '" + first.normalizedFirstName()
                + "' vs '" + second.normalizedFirstName() + "'";

        assert Objects.equals(first.normalizedLastName(), second.normalizedLastName())
                : "lastName mismatch: '" + first.normalizedLastName()
                + "' vs '" + second.normalizedLastName() + "'";

        assert Objects.equals(first.normalizedPhone(), second.normalizedPhone())
                : "phone mismatch: '" + first.normalizedPhone()
                + "' vs '" + second.normalizedPhone() + "'";

        assert Objects.equals(first.normalizedEmail(), second.normalizedEmail())
                : "email mismatch: '" + first.normalizedEmail()
                + "' vs '" + second.normalizedEmail() + "'";

        assert Objects.equals(first.normalizedAddress(), second.normalizedAddress())
                : "address mismatch: '" + first.normalizedAddress()
                + "' vs '" + second.normalizedAddress() + "'";

        assert Objects.equals(first.normalizedDob(), second.normalizedDob())
                : "dob mismatch: '" + first.normalizedDob()
                + "' vs '" + second.normalizedDob() + "'";
    }
}
