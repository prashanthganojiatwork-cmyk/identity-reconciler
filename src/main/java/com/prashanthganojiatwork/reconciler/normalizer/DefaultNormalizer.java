package com.prashanthganojiatwork.reconciler.normalizer;

import com.prashanthganojiatwork.reconciler.model.Address;
import com.prashanthganojiatwork.reconciler.model.NormalizationResult;
import com.prashanthganojiatwork.reconciler.model.NormalizationWarning;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import com.prashanthganojiatwork.reconciler.model.PersonRecord;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.AddressNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.DobNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.EmailNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.NameNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.PhoneNormalizationStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the {@link Normalizer} interface that composes all
 * field-specific normalization strategies.
 *
 * <p>Each field of a {@link PersonRecord} is normalized using the appropriate strategy:
 * <ul>
 *   <li>firstname, middlename, lastname → {@link NameNormalizationStrategy}</li>
 *   <li>phone → {@link PhoneNormalizationStrategy}</li>
 *   <li>email → {@link EmailNormalizationStrategy}</li>
 *   <li>dateOfBirth → {@link DobNormalizationStrategy}</li>
 *   <li>address → {@link AddressNormalizationStrategy}</li>
 * </ul>
 *
 * <p>If a strategy indicates normalization failed (value returned unchanged for non-null input
 * that isn't already canonical), a {@link NormalizationWarning} is collected for that field.</p>
 *
 * <p>Registered as a Spring bean via {@code @Service} annotation for dependency injection (Req 10.4).</p>
 *
 * <p>Satisfies Requirements 2.1-2.7 and 10.4.</p>
 */
@Service
public class DefaultNormalizer implements Normalizer {

    private final NameNormalizationStrategy nameStrategy;
    private final PhoneNormalizationStrategy phoneStrategy;
    private final EmailNormalizationStrategy emailStrategy;
    private final DobNormalizationStrategy dobStrategy;
    private final AddressNormalizationStrategy addressStrategy;

    /**
     * Constructs the DefaultNormalizer with all field normalization strategies injected via Spring DI.
     */
    public DefaultNormalizer(
            NameNormalizationStrategy nameStrategy,
            PhoneNormalizationStrategy phoneStrategy,
            EmailNormalizationStrategy emailStrategy,
            DobNormalizationStrategy dobStrategy,
            AddressNormalizationStrategy addressStrategy) {
        this.nameStrategy = nameStrategy;
        this.phoneStrategy = phoneStrategy;
        this.emailStrategy = emailStrategy;
        this.dobStrategy = dobStrategy;
        this.addressStrategy = addressStrategy;
    }

    @Override
    public NormalizationResult normalize(PersonRecord record) {
        List<NormalizationWarning> warnings = new ArrayList<>();
        Map<String, String> rawValues = new HashMap<>();

        // Normalize firstname
        String normalizedFirstName = normalizeNameField(
                record.firstname(), "firstname", rawValues, warnings);

        // Normalize middlename (stored in raw values but not in NormalizedRecord directly)
        normalizeNameField(record.middlename(), "middlename", rawValues, warnings);

        // Normalize lastname
        String normalizedLastName = normalizeNameField(
                record.lastname(), "lastname", rawValues, warnings);

        // Normalize phone
        String normalizedPhone = normalizePhone(
                record.phone(), rawValues, warnings);

        // Normalize email
        String normalizedEmail = normalizeEmail(
                record.email(), rawValues, warnings);

        // Normalize DOB
        LocalDate normalizedDob = normalizeDob(
                record.dateOfBirth(), rawValues, warnings);

        // Normalize address
        String normalizedAddress = normalizeAddress(
                record.address(), rawValues, warnings);

        // Preserve alternate names
        List<String> alternateNames = record.alternateNames() != null
                ? List.copyOf(record.alternateNames())
                : Collections.emptyList();

        NormalizedRecord normalizedRecord = new NormalizedRecord(
                record.id(),
                record.id(),
                normalizedFirstName,
                normalizedLastName,
                normalizedPhone,
                normalizedEmail,
                normalizedAddress,
                normalizedDob,
                alternateNames,
                Collections.unmodifiableMap(rawValues)
        );

        return new NormalizationResult(normalizedRecord, Collections.unmodifiableList(warnings));
    }

    @Override
    public List<NormalizationResult> normalizeBatch(List<PersonRecord> records) {
        List<NormalizationResult> results = new ArrayList<>(records.size());
        for (PersonRecord record : records) {
            results.add(normalize(record));
        }
        return results;
    }

    /**
     * Normalizes a name field, stores the raw value, and returns the normalized value.
     */
    private String normalizeNameField(String rawValue, String fieldName,
                                       Map<String, String> rawValues,
                                       List<NormalizationWarning> warnings) {
        if (rawValue != null) {
            rawValues.put(fieldName, rawValue);
        }

        // Name normalization always transforms the value (lowercase, trim, etc.)
        // It does not fail in the same way phone/email/dob do, so no warning is needed.
        return nameStrategy.normalize(rawValue);
    }

    /**
     * Normalizes the phone field, stores the raw value, and collects a warning if normalization fails.
     */
    private String normalizePhone(String rawValue, Map<String, String> rawValues,
                                   List<NormalizationWarning> warnings) {
        if (rawValue != null) {
            rawValues.put("phone", rawValue);
        }

        String normalized = phoneStrategy.normalize(rawValue);

        // Check if normalization failed: non-null, non-blank input that is invalid
        if (rawValue != null && !rawValue.isBlank() && !phoneStrategy.isValid(rawValue)) {
            warnings.add(new NormalizationWarning(
                    "phone",
                    rawValue,
                    "Phone number could not be normalized: fewer than 7 digits"
            ));
        }

        return normalized;
    }

    /**
     * Normalizes the email field, stores the raw value, and collects a warning if normalization fails.
     */
    private String normalizeEmail(String rawValue, Map<String, String> rawValues,
                                   List<NormalizationWarning> warnings) {
        if (rawValue != null) {
            rawValues.put("email", rawValue);
        }

        String normalized = emailStrategy.normalize(rawValue);

        // Check if normalization failed: non-null, non-blank input missing "@"
        if (rawValue != null && !rawValue.isBlank() && !emailStrategy.isValid(rawValue)) {
            warnings.add(new NormalizationWarning(
                    "email",
                    rawValue,
                    "Email could not be normalized: missing '@' symbol"
            ));
        }

        return normalized;
    }

    /**
     * Normalizes the DOB field, stores the raw value, and collects a warning if normalization fails.
     */
    private LocalDate normalizeDob(String rawValue, Map<String, String> rawValues,
                                    List<NormalizationWarning> warnings) {
        if (rawValue != null) {
            rawValues.put("dateOfBirth", rawValue);
        }

        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        LocalDate parsed = dobStrategy.parseToLocalDate(rawValue);

        if (parsed == null) {
            // Normalization failed - unparseable date
            warnings.add(new NormalizationWarning(
                    "dateOfBirth",
                    rawValue,
                    "Date of birth could not be parsed in any supported format"
            ));
        }

        return parsed;
    }

    /**
     * Normalizes the address field, stores the raw value, and collects a warning if normalization fails.
     */
    private String normalizeAddress(Address address, Map<String, String> rawValues,
                                     List<NormalizationWarning> warnings) {
        if (address != null) {
            rawValues.put("address", buildRawAddressString(address));
        }

        return addressStrategy.normalize(address);
    }

    /**
     * Builds a raw address string from the Address components for storage in rawValues.
     */
    private String buildRawAddressString(Address address) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, address.streetLine1());
        appendIfPresent(sb, address.streetLine2());
        appendIfPresent(sb, address.city());
        appendIfPresent(sb, address.stateCode());
        appendIfPresent(sb, address.postalCode());
        appendIfPresent(sb, address.countryCode());
        return sb.toString();
    }

    /**
     * Appends a value to the builder with a comma separator if the builder is non-empty.
     */
    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(value);
        }
    }
}
