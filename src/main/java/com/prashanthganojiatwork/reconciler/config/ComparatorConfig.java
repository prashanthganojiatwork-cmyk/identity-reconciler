package com.prashanthganojiatwork.reconciler.config;

import com.prashanthganojiatwork.reconciler.comparator.strategies.AddressSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.DobSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.EmailSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.NameSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.comparator.strategies.PhoneSimilarityStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.NameNormalizationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that registers field similarity strategy beans.
 *
 * <p>The strategy classes themselves are kept free of Spring annotations (Req 10.5),
 * so this configuration class handles their registration as Spring beans for
 * dependency injection into the DefaultFieldComparator (Req 10.4).</p>
 */
@Configuration
public class ComparatorConfig {

    @Bean
    public NameSimilarityStrategy nameSimilarityStrategy(NameNormalizationStrategy nameNormalizationStrategy) {
        return new NameSimilarityStrategy(nameNormalizationStrategy);
    }

    @Bean
    public PhoneSimilarityStrategy phoneSimilarityStrategy() {
        return new PhoneSimilarityStrategy();
    }

    @Bean
    public EmailSimilarityStrategy emailSimilarityStrategy() {
        return new EmailSimilarityStrategy();
    }

    @Bean
    public AddressSimilarityStrategy addressSimilarityStrategy() {
        return new AddressSimilarityStrategy();
    }

    @Bean
    public DobSimilarityStrategy dobSimilarityStrategy() {
        return new DobSimilarityStrategy();
    }
}
