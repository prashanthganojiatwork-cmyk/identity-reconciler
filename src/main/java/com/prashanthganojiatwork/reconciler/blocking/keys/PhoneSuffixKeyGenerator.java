package com.prashanthganojiatwork.reconciler.blocking.keys;

import com.prashanthganojiatwork.reconciler.blocking.BlockingKeyGenerator;
import com.prashanthganojiatwork.reconciler.model.NormalizedRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Generates a blocking key based on the last 4 digits of the normalized phone number.
 * Example: "12065551234" → "PHONE4:1234"
 */
@Component
public class PhoneSuffixKeyGenerator implements BlockingKeyGenerator {

    @Override
    public Set<String> generateKeys(NormalizedRecord record) {
        String phone = record.normalizedPhone();
        if (phone == null || phone.isBlank()) {
            return Collections.emptySet();
        }

        // Extract only digits from the phone
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return Collections.emptySet();
        }

        String last4 = digits.substring(digits.length() - 4);
        return Set.of("PHONE4:" + last4);
    }
}
