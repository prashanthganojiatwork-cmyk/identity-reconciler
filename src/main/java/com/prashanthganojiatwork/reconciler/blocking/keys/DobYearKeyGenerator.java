package com.prashanthganojiatwork.reconciler.blocking.keys;

import com.prashanthganojiatwork.reconciler.blocking.BlockingKeyGenerator;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/**
 * Generates a blocking key based on the year component of the normalized date of birth.
 * Example: DOB 1990-05-15 → "DOBYEAR:1990"
 */
@Component
public class DobYearKeyGenerator implements BlockingKeyGenerator {

    @Override
    public Set<String> generateKeys(NormalizedRecord record) {
        LocalDate dob = record.normalizedDob();
        if (dob == null) {
            return Collections.emptySet();
        }

        return Set.of("DOBYEAR:" + dob.getYear());
    }
}
