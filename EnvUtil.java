package com.biblioteca.util;

import java.util.Optional;

/** Small helper to read environment variables. */
public final class EnvUtil {

    // Utility class: it should not be instantiated.
    private EnvUtil() {
    }

    /** Returns the value of an environment variable, or an empty Optional when it is missing/blank. */
    public static Optional<String> get(String name) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
    }
}
