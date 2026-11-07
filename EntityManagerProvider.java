package com.biblioteca.persistence;

import com.biblioteca.util.EnvUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Single place in the application that knows how to obtain an EntityManager.
 * Every repository calls EntityManagerProvider.getEntityManager() (this replaces
 * ConnectionFactory.getConnection()).
 *
 * The database settings (URL, user, password) live in META-INF/persistence.xml and can be
 * overridden with environment variables (DB_URL, DB_USER, DB_PASSWORD), which keeps real
 * credentials out of the source code and out of Git.
 */
public final class EntityManagerProvider {

    // Must match the name of the <persistence-unit> in META-INF/persistence.xml.
    private static final String PERSISTENCE_UNIT = "bibliotecaPU";

    // Creating an EntityManagerFactory is expensive (Hibernate reads the mappings and prepares
    // the connection pool), so it is created once and shared. It is created lazily, on first use,
    // so the menu still opens when the database is down and the error is reported to the user
    // when an operation needs the database, just like it was with ConnectionFactory.
    private static EntityManagerFactory factory;

    // Utility class: it should not be instantiated.
    private EntityManagerProvider() {
    }

    /**
     * Opens a NEW EntityManager every time it is called.
     * The caller is responsible for closing it (use try-with-resources).
     */
    public static EntityManager getEntityManager() {
        return getFactory().createEntityManager();
    }

    /** Releases the connection pool. Call it once, when the application ends. */
    public static synchronized void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        factory = null;
    }

    private static synchronized EntityManagerFactory getFactory() {
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, environmentOverrides());
        }
        return factory;
    }

    // Values found in the environment win over the defaults written in persistence.xml.
    private static Map<String, String> environmentOverrides() {
        Map<String, String> overrides = new HashMap<>();
        EnvUtil.get("DB_URL").ifPresent(value -> overrides.put("jakarta.persistence.jdbc.url", value));
        EnvUtil.get("DB_USER").ifPresent(value -> overrides.put("jakarta.persistence.jdbc.user", value));
        EnvUtil.get("DB_PASSWORD").ifPresent(value -> overrides.put("jakarta.persistence.jdbc.password", value));
        return overrides;
    }
}
