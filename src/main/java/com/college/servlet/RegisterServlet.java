package com.college.servlet;

import com.college.util.ActivityLogger;
import com.college.util.DBConnection;
import com.college.util.JsonUtil;
import com.college.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/** POST /api/register  ->  creates a new USER account and logs them in. */
@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");

        // 1. Read the form values
        String name = clean(req.getParameter("name"));
        String email = clean(req.getParameter("email")).toLowerCase();
        String password = req.getParameter("password");
        if (password == null) password = "";

        // 2. Validate
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "All fields are required."));
            return;
        }
        if (name.length() > 100 || email.length() > 100) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Name or email is too long."));
            return;
        }
        if (!email.contains("@")) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Enter a valid email."));
            return;
        }
        if (password.length() < 6) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Password must be at least 6 characters."));
            return;
        }

        // 3. Talk to the database
        try (Connection con = DBConnection.getConnection()) {

            // Is the email already used?
            try (PreparedStatement check = con.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                check.setString(1, email);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        JsonUtil.send(res, 409, JsonUtil.message(false, "This email is already registered."));
                        return;
                    }
                }
            }

            // Insert the new user (password is stored as a hash)
            int newId;
            String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'USER')";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, PasswordUtil.hash(password));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    newId = keys.getInt(1);
                }
            }

            ActivityLogger.log(con, newId, "REGISTERED");

            // Automatically start session for immediate login
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("userId", newId);
            session.setAttribute("name", name);
            session.setAttribute("role", "USER");

            JsonUtil.send(res, 200, Map.of(
                "success", true,
                "message", "Account created successfully.",
                "role", "USER",
                "name", name
            ));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
        }
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
