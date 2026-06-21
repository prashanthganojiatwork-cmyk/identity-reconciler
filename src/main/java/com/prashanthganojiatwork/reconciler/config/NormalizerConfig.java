package com.prashanthganojiatwork.reconciler.config;

import com.prashanthganojiatwork.reconciler.normalizer.strategies.AddressNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.DobNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.EmailNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.NameNormalizationStrategy;
import com.prashanthganojiatwork.reconciler.normalizer.strategies.PhoneNormalizationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that registers normalization strategy beans.
 *
 * <p>The strategy classes themselves are kept free of Spring annotations (Req 10.5),
 * so this configuration class handles their registration as Spring beans for
 * dependency injection into the DefaultNormalizer (Req 10.4).</p>
 */
@Configuration
public class NormalizerConfig {

    @Bean
    public NameNormalizationStrategy nameNormalizationStrategy() {
        return new NameNormalizationStrategy();
    }

    @Bean
    public PhoneNormalizationStrategy phoneNormalizationStrategy() {
        return new PhoneNormalizationStrategy();
    }

    @Bean
    public EmailNormalizationStrategy emailNormalizationStrategy() {
        return new EmailNormalizationStrategy();
    }

    @Bean
    public DobNormalizationStrategy dobNormalizationStrategy() {
        return new DobNormalizationStrategy();
    }

    @Bean
    public AddressNormalizationStrategy addressNormalizationStrategy() {
        return new AddressNormalizationStrategy();
    }
}
