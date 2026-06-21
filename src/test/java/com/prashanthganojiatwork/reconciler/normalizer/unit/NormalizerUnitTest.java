package com.prashanthganojiatwork.reconciler.normalizer.unit;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Normalizer component, covering all normalization strategies
 * and the DefaultNormalizer integration.
 *
 * Validates: Requirements 2.1-2.7
 */
class NormalizerUnitTest {

    private NameNormalizationStrategy nameStrategy;
    private PhoneNormalizationStrategy phoneStrategy;
    private EmailNormalizationStrategy emailStrategy;
    private DobNormalizationStrategy dobStrategy;
    private AddressNormalizationStrategy addressStrategy;
    private DefaultNormalizer normalizer;

    @BeforeEach
    void setUp() {
        nameStrategy = new NameNormalizationStrategy();
        phoneStrategy = new PhoneNormalizationStrategy();
        emailStrategy = new EmailNormalizationStrategy();
        dobStrategy = new DobNormalizationStrategy();
        addressStrategy = new AddressNormalizationStrategy();
        normalizer = new DefaultNormalizer(
                nameStrategy, phoneStrategy, emailStrategy, dobStrategy, addressStrategy);
    }

    // =========================================================================
    // 1. Name Normalization Tests
    // =========================================================================

    @Nested
    @DisplayName("Name Normalization")
    class NameNormalizationTests {

        @Test
        @DisplayName("'Bill' is lowercased to 'bill' (no nickname resolution in normalizer)")
        void bill_lowercasedOnly() {
            String result = nameStrategy.normalize("Bill");
            assertEquals("bill", result);
        }

        @Test
        @DisplayName("'Bob' is lowercased to 'bob' (no nickname resolution in normalizer)")
        void bob_lowercasedOnly() {
            String result = nameStrategy.normalize("Bob");
            assertEquals("bob", result);
        }

        @Test
        @DisplayName("'William' is lowercased to 'william' (no change beyond lowercasing)")
        void william_lowercased() {
            String result = nameStrategy.normalize("William");
            assertEquals("william", result);
        }

        @Test
        @DisplayName("Hyphens retained: O'Brien keeps the apostrophe")
        void apostropheRetained() {
            String result = nameStrategy.normalize("O'Brien");
            assertEquals("o'brien", result);
        }

        @Test
        @DisplayName("Hyphens retained: Smith-Jones keeps the hyphen")
        void hyphenRetained() {
            String result = nameStrategy.normalize("Smith-Jones");
            assertEquals("smith-jones", result);
        }

        @Test
        @DisplayName("Periods removed: 'Jr.' → 'jr'")
        void periodsRemoved() {
            String result = nameStrategy.normalize("Jr.");
            assertEquals("jr", result);
        }

        @Test
        @DisplayName("Whitespace collapse: '  John   Doe  ' → 'john doe' (trimmed, collapsed)")
        void whitespaceCollapse() {
            String result = nameStrategy.normalize("  John   Doe  ");
            assertEquals("john doe", result);
        }

        @Test
        @DisplayName("Null first name → null in normalized output (no exception)")
        void nullName() {
            String result = nameStrategy.normalize(null);
            assertNull(result);
        }

        @Test
        @DisplayName("getCanonicalName still resolves nicknames for comparator use")
        void getCanonicalName_resolvesNicknames() {
            assertEquals("william", nameStrategy.getCanonicalName("Bill"));
            assertEquals("robert", nameStrategy.getCanonicalName("Bob"));
            assertEquals("william", nameStrategy.getCanonicalName("william"));
        }
    }

    // =========================================================================
    // 2. Phone Normalization Tests
    // =========================================================================

    @Nested
    @DisplayName("Phone Normalization")
    class PhoneNormalizationTests {

        @Test
        @DisplayName("'(206) 555-1234' → '12065551234'")
        void phoneWithParens() {
            String result = phoneStrategy.normalize("(206) 555-1234");
            assertEquals("12065551234", result);
        }

        @Test
        @DisplayName("'206.555.1234' → '12065551234'")
        void phoneWithDots() {
            String result = phoneStrategy.normalize("206.555.1234");
            assertEquals("12065551234", result);
        }

        @Test
        @DisplayName("'+1-206-555-1234' → '12065551234'")
        void phoneWithCountryCode() {
            String result = phoneStrategy.normalize("+1-206-555-1234");
            assertEquals("12065551234", result);
        }

        @Test
        @DisplayName("'5551234' (7 digits, no country code prepended) → '5551234'")
        void phoneSevenDigits() {
            String result = phoneStrategy.normalize("5551234");
            assertEquals("5551234", result);
        }

        @Test
        @DisplayName("'123' (fewer than 7 digits) → original value returned, warning generated")
        void phoneFewerThanSevenDigits() {
            String result = phoneStrategy.normalize("123");
            assertEquals("123", result);
            assertFalse(phoneStrategy.isValid("123"));
        }

        @Test
        @DisplayName("Null phone → null, no warning")
        void nullPhone() {
            String result = phoneStrategy.normalize(null);
            assertNull(result);
        }
    }

    // =========================================================================
    // 3. Email Normalization Tests
    // =========================================================================

    @Nested
    @DisplayName("Email Normalization")
    class EmailNormalizationTests {

        @Test
        @DisplayName("'User+Tag@Example.COM' → 'user@example.com'")
        void emailWithPlusTag() {
            String result = emailStrategy.normalize("User+Tag@Example.COM");
            assertEquals("user@example.com", result);
        }

        @Test
        @DisplayName("'JOHN.DOE@GMAIL.COM' → 'john.doe@gmail.com'")
        void emailUppercase() {
            String result = emailStrategy.normalize("JOHN.DOE@GMAIL.COM");
            assertEquals("john.doe@gmail.com", result);
        }

        @Test
        @DisplayName("'test+marketing+extra@domain.org' → 'test@domain.org'")
        void emailMultiplePlusTags() {
            String result = emailStrategy.normalize("test+marketing+extra@domain.org");
            assertEquals("test@domain.org", result);
        }

        @Test
        @DisplayName("'invalid-no-at-sign' → original value returned, warning generated")
        void invalidEmail() {
            String result = emailStrategy.normalize("invalid-no-at-sign");
            assertEquals("invalid-no-at-sign", result);
            assertFalse(emailStrategy.isValid("invalid-no-at-sign"));
        }

        @Test
        @DisplayName("Null email → null, no warning")
        void nullEmail() {
            String result = emailStrategy.normalize(null);
            assertNull(result);
        }
    }

    // =========================================================================
    // 4. DOB Normalization Tests
    // =========================================================================

    @Nested
    @DisplayName("DOB Normalization")
    class DobNormalizationTests {

        @Test
        @DisplayName("'1990-05-15' (ISO) → '1990-05-15'")
        void dobIso() {
            String result = dobStrategy.normalize("1990-05-15");
            assertEquals("1990-05-15", result);
        }

        @Test
        @DisplayName("'05/15/1990' (MM/DD/YYYY) → '1990-05-15'")
        void dobMmDdYyyy() {
            String result = dobStrategy.normalize("05/15/1990");
            assertEquals("1990-05-15", result);
        }

        @Test
        @DisplayName("'15-05-1990' (DD-MM-YYYY) → '1990-05-15'")
        void dobDdMmYyyy() {
            String result = dobStrategy.normalize("15-05-1990");
            assertEquals("1990-05-15", result);
        }

        @Test
        @DisplayName("'5/3/1990' (M/D/YYYY) → '1990-05-03'")
        void dobSingleDigit() {
            String result = dobStrategy.normalize("5/3/1990");
            assertEquals("1990-05-03", result);
        }

        @Test
        @DisplayName("'1990/05/15' (YYYY/MM/DD) → '1990-05-15'")
        void dobYyyyMmDd() {
            String result = dobStrategy.normalize("1990/05/15");
            assertEquals("1990-05-15", result);
        }

        @Test
        @DisplayName("'January 5, 1990' → '1990-01-05'")
        void dobMonthName() {
            String result = dobStrategy.normalize("January 5, 1990");
            assertEquals("1990-01-05", result);
        }

        @Test
        @DisplayName("'13/45/2000' (invalid) → original value returned, warning generated")
        void dobInvalid() {
            String result = dobStrategy.normalize("13/45/2000");
            assertEquals("13/45/2000", result);
            assertFalse(dobStrategy.isValid("13/45/2000"));
        }

        @Test
        @DisplayName("Null DOB → null, no warning")
        void nullDob() {
            String result = dobStrategy.normalize(null);
            assertNull(result);
        }
    }

    // =========================================================================
    // 5. Address Normalization Tests
    // =========================================================================

    @Nested
    @DisplayName("Address Normalization")
    class AddressNormalizationTests {

        @Test
        @DisplayName("Address with '100 Main St' → '100 main street'")
        void addressStreetAbbreviation() {
            Address address = new Address("100 Main St", null, null, null, null, null);
            String result = addressStrategy.normalize(address);
            assertEquals("100 main street", result);
        }

        @Test
        @DisplayName("Address with '200 Oak Ave, Apt 3' → contains '200 oak avenue' and 'apartment 3'")
        void addressAvenueAndApt() {
            Address address = new Address("200 Oak Ave", "Apt 3", null, null, null, null);
            String result = addressStrategy.normalize(address);
            assertNotNull(result);
            assertTrue(result.contains("200 oak avenue"), "Expected '200 oak avenue' in: " + result);
            assertTrue(result.contains("apartment 3"), "Expected 'apartment 3' in: " + result);
        }

        @Test
        @DisplayName("Null address → null output")
        void nullAddress() {
            String result = addressStrategy.normalize(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Address with all null fields → null output")
        void addressAllNullFields() {
            Address address = new Address(null, null, null, null, null, null);
            String result = addressStrategy.normalize(address);
            assertNull(result);
        }
    }

    // =========================================================================
    // 6. DefaultNormalizer Integration Tests
    // =========================================================================

    @Nested
    @DisplayName("DefaultNormalizer Integration")
    class DefaultNormalizerIntegrationTests {

        @Test
        @DisplayName("Full PersonRecord with all fields → NormalizationResult with normalized values and empty warnings")
        void fullRecordNormalization() {
            PersonRecord record = new PersonRecord(
                    "rec-001",
                    "Bill",
                    "James",
                    "Smith",
                    List.of("Billy S"),
                    "05/15/1990",
                    "(206) 555-1234",
                    "User+Tag@Example.COM",
                    new Address("100 Main St", null, "Seattle", "WA", "98101", "US")
            );

            NormalizationResult result = normalizer.normalize(record);

            assertNotNull(result);
            assertNotNull(result.normalizedRecord());
            assertTrue(result.warnings().isEmpty(),
                    "Expected no warnings but got: " + result.warnings());

            NormalizedRecord normalized = result.normalizedRecord();
            assertEquals("bill", normalized.normalizedFirstName());
            assertEquals("smith", normalized.normalizedLastName());
            assertEquals("12065551234", normalized.normalizedPhone());
            assertEquals("user@example.com", normalized.normalizedEmail());
            assertEquals(LocalDate.of(1990, 5, 15), normalized.normalizedDob());
            assertNotNull(normalized.normalizedAddress());
            assertTrue(normalized.normalizedAddress().contains("100 main street"));
            assertEquals(List.of("Billy S"), normalized.alternateNames());
        }

        @Test
        @DisplayName("PersonRecord with some null fields → processed without error, missing fields are null in output")
        void partialRecordNormalization() {
            PersonRecord record = new PersonRecord(
                    "rec-002",
                    "John",
                    null,
                    "Doe",
                    null,
                    null,
                    null,
                    "john@example.com",
                    null
            );

            NormalizationResult result = normalizer.normalize(record);

            assertNotNull(result);
            assertNotNull(result.normalizedRecord());
            assertTrue(result.warnings().isEmpty());

            NormalizedRecord normalized = result.normalizedRecord();
            assertEquals("john", normalized.normalizedFirstName());
            assertEquals("doe", normalized.normalizedLastName());
            assertNull(normalized.normalizedPhone());
            assertEquals("john@example.com", normalized.normalizedEmail());
            assertNull(normalized.normalizedDob());
            assertNull(normalized.normalizedAddress());
            assertTrue(normalized.alternateNames().isEmpty());
        }

        @Test
        @DisplayName("PersonRecord with all invalid values → NormalizationResult with original values and multiple warnings")
        void allInvalidFieldsProduceWarnings() {
            PersonRecord record = new PersonRecord(
                    "rec-003",
                    "John",
                    null,
                    "Doe",
                    null,
                    "13/45/2000",
                    "123",
                    "invalid-no-at-sign",
                    null
            );

            NormalizationResult result = normalizer.normalize(record);

            assertNotNull(result);
            assertNotNull(result.normalizedRecord());

            List<NormalizationWarning> warnings = result.warnings();
            assertFalse(warnings.isEmpty(), "Expected warnings for invalid fields");

            assertTrue(warnings.stream().anyMatch(w -> w.fieldName().equals("phone")),
                    "Expected warning for phone field");
            assertTrue(warnings.stream().anyMatch(w -> w.fieldName().equals("email")),
                    "Expected warning for email field");
            assertTrue(warnings.stream().anyMatch(w -> w.fieldName().equals("dateOfBirth")),
                    "Expected warning for dateOfBirth field");

            NormalizedRecord normalized = result.normalizedRecord();
            assertEquals("123", normalized.normalizedPhone());
            assertEquals("invalid-no-at-sign", normalized.normalizedEmail());
            assertNull(normalized.normalizedDob());
        }
    }
}
