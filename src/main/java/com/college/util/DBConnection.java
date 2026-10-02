package com.college.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates a JDBC connection. Works with MySQL (local) and PostgreSQL (Render).
 *
 * 1) If the environment variable DATABASE_URL is set (Render gives this),
 *    we connect to PostgreSQL using it.
 * 2) Otherwise we connect to MySQL using DB_URL / DB_USER / DB_PASSWORD,
 *    falling back to the local defaults below.
 *
 * Local run: set DB_PASSWORD before starting Tomcat, or change the default below.
 * Never commit your real password to GitHub.
 */
public class DBConnection {

    private static final String MYSQL_URL = env("DB_URL",
        "jdbc:mysql://localhost:3306/complaint_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String MYSQL_USER = env("DB_USER", "root");
    private static final String MYSQL_PASSWORD = env("DB_PASSWORD", "your_mysql_password");

    public static Connection getConnection() throws SQLException {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return connectPostgres(databaseUrl.trim());
        }
        return connectMySql();
    }

    private static Connection connectMySql() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found. Check pom.xml", e);
        }
        return DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
    }

    /** DATABASE_URL looks like: postgres://user:password@host:5432/dbname */
    private static Connection connectPostgres(String databaseUrl) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            URI uri = new URI(databaseUrl);

            String[] userInfo = uri.getRawUserInfo().split(":", 2);
            String user = URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8);
            String password = URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port
                           + uri.getPath() + "?sslmode=prefer";
            return DriverManager.getConnection(jdbcUrl, user, password);

        } catch (ClassNotFoundException | URISyntaxException | RuntimeException e) {
            throw new SQLException("Could not use DATABASE_URL: " + e.getMessage(), e);
        }
    }

    /** Reads an environment variable, or returns the fallback if it is missing/empty. */
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
