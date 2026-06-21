package com.prashanthganojiatwork.reconciler.comparator;

import com.prashanthganojiatwork.reconciler.comparator.strategies.AddressSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.DobSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.EmailSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.NameSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.PhoneSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.model.FieldComparisonResult;
import com.prashanthganojiatwork.reconciler.model.FieldScore;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of the {@link FieldComparator} interface that composes all
 * field-specific similarity strategies to produce a complete comparison result.
 *
 * <p>For each field pair (firstName, lastName, phone, email, address, dob), this
 * comparator:
 * <ul>
 *   <li>If BOTH values are null/empty → marks as NOT present, adds to missingFields</li>
 *   <li>If one value is null/empty and the other is not → marks as present with similarity 0.0</li>
 *   <li>If both values are present → computes similarity using the appropriate strategy</li>
 * </ul>
 *
 * <p>Registered as a Spring bean via {@code @Service} annotation for dependency injection (Req 10.4).</p>
 *
 * <p>Satisfies Requirements 3.1, 3.6, 10.1, 10.4.</p>
 */
@Service
public class DefaultFieldComparator implements FieldComparator {

    private final NameSimilarityStrategy nameSimilarityStrategy;
    private final PhoneSimilarityStrategy phoneSimilarityStrategy;
    private final EmailSimilarityStrategy emailSimilarityStrategy;
    private final AddressSimilarityStrategy addressSimilarityStrategy;
    private final DobSimilarityStrategy dobSimilarityStrategy;

    /**
     * Constructs the DefaultFieldComparator with all similarity strategies injected via Spring DI.
     */
    public DefaultFieldComparator(
            NameSimilarityStrategy nameSimilarityStrategy,
            PhoneSimilarityStrategy phoneSimilarityStrategy,
            EmailSimilarityStrategy emailSimilarityStrategy,
            AddressSimilarityStrategy addressSimilarityStrategy,
            DobSimilarityStrategy dobSimilarityStrategy) {
        this.nameSimilarityStrategy = nameSimilarityStrategy;
        this.phoneSimilarityStrategy = phoneSimilarityStrategy;
        this.emailSimilarityStrategy = emailSimilarityStrategy;
        this.addressSimilarityStrategy = addressSimilarityStrategy;
        this.dobSimilarityStrategy = dobSimilarityStrategy;
    }

    @Override
    public FieldComparisonResult compare(NormalizedRecord recordA, NormalizedRecord recordB) {
        List<FieldScore> fieldScores = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        // Compare firstName
        compareField(
                "firstName",
                recordA.normalizedFirstName(),
                recordB.normalizedFirstName(),
                getRawValue(recordA, "firstname"),
                getRawValue(recordB, "firstname"),
                nameSimilarityStrategy,
                fieldScores,
                missingFields
        );

        // Compare lastName
        compareField(
                "lastName",
                recordA.normalizedLastName(),
                recordB.normalizedLastName(),
                getRawValue(recordA, "lastname"),
                getRawValue(recordB, "lastname"),
                nameSimilarityStrategy,
                fieldScores,
                missingFields
        );

        // Compare phone
        compareField(
                "phone",
                recordA.normalizedPhone(),
                recordB.normalizedPhone(),
                getRawValue(recordA, "phone"),
                getRawValue(recordB, "phone"),
                phoneSimilarityStrategy,
                fieldScores,
                missingFields
        );

        // Compare email
        compareField(
                "email",
                recordA.normalizedEmail(),
                recordB.normalizedEmail(),
                getRawValue(recordA, "email"),
                getRawValue(recordB, "email"),
                emailSimilarityStrategy,
                fieldScores,
                missingFields
        );

        // Compare address
        compareField(
                "address",
                recordA.normalizedAddress(),
                recordB.normalizedAddress(),
                getRawValue(recordA, "address"),
                getRawValue(recordB, "address"),
                addressSimilarityStrategy,
                fieldScores,
                missingFields
        );

        // Compare dob (LocalDate → String for comparison)
        String dobA = recordA.normalizedDob() != null ? recordA.normalizedDob().toString() : null;
        String dobB = recordB.normalizedDob() != null ? recordB.normalizedDob().toString() : null;
        compareField(
                "dob",
                dobA,
                dobB,
                getRawValue(recordA, "dateOfBirth"),
                getRawValue(recordB, "dateOfBirth"),
                dobSimilarityStrategy,
                fieldScores,
                missingFields
        );

        return new FieldComparisonResult(
                Collections.unmodifiableList(fieldScores),
                Collections.unmodifiableList(missingFields)
        );
    }

    /**
     * Compares a single field between two records and adds the result to the appropriate list.
     *
     * <p>Logic:
     * <ul>
     *   <li>Both null/empty → not present, added to missingFields</li>
     *   <li>One null/empty, one present → present=true, similarity=0.0</li>
     *   <li>Both present → compute similarity using strategy</li>
     * </ul>
     */
    private void compareField(
            String fieldName,
            String normalizedA,
            String normalizedB,
            String rawA,
            String rawB,
            FieldSimilarityStrategy strategy,
            List<FieldScore> fieldScores,
            List<String> missingFields) {

        boolean aEmpty = isNullOrEmpty(normalizedA);
        boolean bEmpty = isNullOrEmpty(normalizedB);

        if (aEmpty && bEmpty) {
            // Both values are null/empty — mark as NOT present, add to missingFields
            fieldScores.add(new FieldScore(
                    fieldName,
                    0.0,
                    rawA,
                    rawB,
                    normalizedA,
                    normalizedB,
                    "Not compared (both values missing)",
                    false
            ));
            missingFields.add(fieldName);
        } else if (aEmpty || bEmpty) {
            // One value is null/empty and the other is not — present with similarity 0.0
            String comparisonMethod = aEmpty
                    ? "Source A value missing"
                    : "Source B value missing";
            fieldScores.add(new FieldScore(
                    fieldName,
                    0.0,
                    rawA,
                    rawB,
                    normalizedA,
                    normalizedB,
                    comparisonMethod,
                    true
            ));
        } else {
            // Both values are present — compute similarity using the strategy
            double similarity = strategy.computeSimilarity(normalizedA, normalizedB);
            String comparisonMethod = strategy.describeComparison(normalizedA, normalizedB);
            fieldScores.add(new FieldScore(
                    fieldName,
                    similarity,
                    rawA,
                    rawB,
                    normalizedA,
                    normalizedB,
                    comparisonMethod,
                    true
            ));
        }
    }

    /**
     * Retrieves a raw value from the record's rawValues map.
     */
    private String getRawValue(NormalizedRecord record, String fieldName) {
        if (record.rawValues() == null) {
            return null;
        }
        return record.rawValues().get(fieldName);
    }

    /**
     * Checks if a string value is null or effectively empty (blank).
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.isBlank();
    }
}
