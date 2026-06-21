package com.prashanthganojiatwork.reconciler.model;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Input person record modeled after Trestle's Find Person API person object.
 * Fields are nullable to support partial records (Req 1.3).
 */
public record PersonRecord(
    String id,
    @Nullable String firstname,
    @Nullable String middlename,
    @Nullable String lastname,
    @Nullable List<String> alternateNames,
    @Nullable String dateOfBirth,
    @Nullable String phone,
    @Nullable String email,
    @Nullable Address address
) {}
