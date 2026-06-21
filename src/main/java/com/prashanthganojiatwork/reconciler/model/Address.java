package com.prashanthganojiatwork.reconciler.model;

import jakarta.annotation.Nullable;

/**
 * Structured address representation aligned with Trestle Location format.
 */
public record Address(
    @Nullable String streetLine1,
    @Nullable String streetLine2,
    @Nullable String city,
    @Nullable String stateCode,
    @Nullable String postalCode,
    @Nullable String countryCode
) {}
