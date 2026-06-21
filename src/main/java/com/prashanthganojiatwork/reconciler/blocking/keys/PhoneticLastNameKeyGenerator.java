package com.prashanthganojiatwork.reconciler.blocking.keys;

import com.prashanthganojiatwork.reconciler.blocking.BlockingKeyGenerator;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.apache.commons.codec.language.Soundex;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Generates a blocking key based on Soundex encoding of the normalized last name.
 * Example: "Smith" → "SOUNDEX:S530", "Johnson" → "SOUNDEX:J525"
 */
@Component
public class PhoneticLastNameKeyGenerator implements BlockingKeyGenerator {

    private final Soundex soundex = new Soundex();

    @Override
    public Set<String> generateKeys(NormalizedRecord record) {
        String lastName = record.normalizedLastName();
        if (lastName == null || lastName.isBlank()) {
            return Collections.emptySet();
        }

        try {
            String soundexCode = soundex.soundex(lastName);
            return Set.of("SOUNDEX:" + soundexCode);
        } catch (IllegalArgumentException e) {
            // If the name cannot be encoded (e.g., contains no mappable characters)
            return Collections.emptySet();
        }
    }
}
