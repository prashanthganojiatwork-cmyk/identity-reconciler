package com.prashanthganojiatwork.reconciler.blocking.keys;

import com.prashanthganojiatwork.reconciler.blocking.BlockingKeyGenerator;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/**
 * Generates a blocking key by combining the first character of the normalized first name
 * with the zero-padded month from the normalized date of birth.
 * Example: firstName "john", DOB 1990-05-15 → "INITMO:j05"
 */
@Component
public class FirstInitialDobMonthKeyGenerator implements BlockingKeyGenerator {

    @Override
    public Set<String> generateKeys(NormalizedRecord record) {
        String firstName = record.normalizedFirstName();
        LocalDate dob = record.normalizedDob();

        if (firstName == null || firstName.isBlank() || dob == null) {
            return Collections.emptySet();
        }

        char firstChar = firstName.charAt(0);
        String monthPadded = String.format("%02d", dob.getMonthValue());
        return Set.of("INITMO:" + firstChar + monthPadded);
    }
}
