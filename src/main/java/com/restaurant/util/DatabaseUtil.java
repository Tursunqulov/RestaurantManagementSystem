package com.restaurant.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton utility for managing the JDBC connection to MySQL/MariaDB.
 *
 * <h3>Connection resolution order:</h3>
 * <ol>
 *   <li>Environment variables: {@code DB_URL}, {@code DB_USER}, {@code DB_PASS}</li>
 *   <li>Safe defaults for a local XAMPP installation (root / no password)</li>
 * </ol>
 *
 * <h3>OOP &amp; SOLID principles applied:</h3>
 * <ul>
 *   <li><b>Encapsulation</b> — all connection internals are private.</li>
 *   <li><b>Single Responsibility (S)</b> — this class handles connectivity only.</li>
 *   <li><b>Open/Closed (O)</b> — behaviour is configurable via env vars without
 *       modifying source code.</li>
 * </ul>
 *
 * @author Restaurant Management System
 */
public final class DatabaseUtil {

    // ── Defaults (XAMPP out-of-the-box) ──────────────────────
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/restaurant_ms"
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "restaurant";
    private static final String DEFAULT_PASS = "restaurant123";

    // ── Resolved at class-load time ─────────────────────────
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASS;

    static {
        DB_URL  = env("DB_URL",  DEFAULT_URL);
        DB_USER = env("DB_USER", DEFAULT_USER);
        DB_PASS = env("DB_PASS", DEFAULT_PASS);
    }

    // ── Singleton state ─────────────────────────────────────
    private static volatile DatabaseUtil instance;
    private Connection connection;

    // ── Private constructor ─────────────────────────────────
    private DatabaseUtil() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver not found. Add mysql-connector-j to classpath.", e);
        }
        this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ── Public API ──────────────────────────────────────────

    /**
     * Returns a live JDBC {@link Connection}, creating or re-creating it as needed.
     *
     * @return an open {@link Connection}
     * @throws SQLException if a connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
            synchronized (DatabaseUtil.class) {
                if (instance == null || instance.connection == null || instance.connection.isClosed()) {
                    instance = new DatabaseUtil();
                }
            }
        }
        return instance.connection;
    }

    /**
     * Gracefully closes the current connection (call on application shutdown).
     */
    public static void closeConnection() {
        synchronized (DatabaseUtil.class) {
            if (instance != null && instance.connection != null) {
                try {
                    if (!instance.connection.isClosed()) {
                        instance.connection.close();
                    }
                } catch (SQLException e) {
                    System.err.println("[DatabaseUtil] Error closing connection: " + e.getMessage());
                } finally {
                    instance = null;
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /** Smoke-test — run this main method to verify your DB setup. */
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            System.out.println("Database connection test PASSED.");
            System.out.println("  Catalog : " + conn.getCatalog());
            System.out.println("  URL     : " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("Database connection test FAILED: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
}
