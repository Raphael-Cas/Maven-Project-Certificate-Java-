package com.biblioteca.persistence;

import jakarta.persistence.PersistenceException;
import org.hibernate.JDBCException;

/**
 * Thrown by the repositories when the database operation fails.
 *
 * It takes the place of SQLException in the application: the menu (Main) catches it and
 * reads the SQLState and the message exactly as it did before, without having to know
 * anything about JPA or Hibernate exceptions.
 */
public class DatabaseException extends RuntimeException {

    private final String sqlState;

    public DatabaseException(String message, String sqlState, Throwable cause) {
        super(message, cause);
        this.sqlState = sqlState;
    }

    /** SQLState reported by PostgreSQL (e.g. "23503" = foreign key violation), or null if unknown. */
    public String getSQLState() {
        return sqlState;
    }

    /**
     * Converts a JPA exception into a DatabaseException, keeping the original message and
     * SQLState sent by PostgreSQL. Hibernate wraps the original error one or more levels deep.
     */
    public static DatabaseException from(PersistenceException e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof JDBCException jdbcException) {
                String message = jdbcException.getCause() != null
                        ? jdbcException.getCause().getMessage()
                        : jdbcException.getMessage();
                return new DatabaseException(message, jdbcException.getSQLState(), e);
            }
            current = current.getCause();
        }
        return new DatabaseException(e.getMessage(), null, e);
    }
}
