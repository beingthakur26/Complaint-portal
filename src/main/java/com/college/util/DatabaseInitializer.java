package com.college.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs once when the app starts. Creates the 3 tables if they do not exist
 * and creates the default admin account if it is missing.
 * This is why a fresh hosted database needs no manual SQL step.
 *
 * Optional environment variable ADMIN_PASSWORD sets the password of the
 * first admin account (default: admin123).
 */
@WebListener
public class DatabaseInitializer implements ServletContextListener {

    private static final String ADMIN_EMAIL = "admin@college.com";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            // The only difference between MySQL and PostgreSQL here is the auto-increment id
            boolean postgres = con.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
            String idColumn = postgres ? "id SERIAL PRIMARY KEY" : "id INT PRIMARY KEY AUTO_INCREMENT";

            st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                + idColumn + ", "
                + "name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(100) UNIQUE NOT NULL, "
                + "password VARCHAR(255) NOT NULL, "
                + "role VARCHAR(10) NOT NULL DEFAULT 'USER')");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS complaints ("
                + idColumn + ", "
                + "user_id INT NOT NULL, "
                + "title VARCHAR(150) NOT NULL, "
                + "description TEXT NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS activity_log ("
                + idColumn + ", "
                + "user_id INT, "
                + "action VARCHAR(255) NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id))");

            createAdminIfMissing(con);
            System.out.println("[DatabaseInitializer] Database ready (" +
                con.getMetaData().getDatabaseProductName() + ").");

        } catch (SQLException e) {
            // Do not crash the whole app; the error will also show on login/register
            System.err.println("[DatabaseInitializer] Could not prepare the database: " + e.getMessage());
        }
    }

    private void createAdminIfMissing(Connection con) throws SQLException {
        try (PreparedStatement check = con.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            check.setString(1, ADMIN_EMAIL);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return;   // admin already exists
            }
        }

        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) adminPassword = "admin123";

        try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'ADMIN')")) {
            ins.setString(1, "Administrator");
            ins.setString(2, ADMIN_EMAIL);
            ins.setString(3, PasswordUtil.hash(adminPassword));
            ins.executeUpdate();
        }
    }
}
