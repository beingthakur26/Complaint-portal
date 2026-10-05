package com.college.servlet;

import com.college.util.ActivityLogger;
import com.college.util.DBConnection;
import com.college.util.JsonUtil;
import com.college.util.PasswordUtil;
import com.college.util.SessionUtil;
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
import java.util.Map;

/**
 * GET  /api/profile -> returns user's profile info
 * POST /api/profile -> updates user's name, email, or password
 */
@WebServlet("/api/profile")
public class ProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Integer userId = SessionUtil.getUserId(req);
        if (userId == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Please log in."));
            return;
        }

        String sql = "SELECT id, name, email, role FROM users WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonUtil.send(res, 200, Map.of(
                        "success", true,
                        "id", rs.getInt("id"),
                        "name", rs.getString("name"),
                        "email", rs.getString("email"),
                        "role", rs.getString("role")
                    ));
                    return;
                }
            }
            JsonUtil.send(res, 404, JsonUtil.message(false, "User not found."));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");

        Integer userId = SessionUtil.getUserId(req);
        if (userId == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Please log in."));
            return;
        }

        String name = req.getParameter("name");
        String email = req.getParameter("email");
        String newPassword = req.getParameter("newPassword");

        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase();
        newPassword = newPassword == null ? "" : newPassword.trim();

        if (name.isEmpty() || email.isEmpty()) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Name and email are required."));
            return;
        }

        if (name.length() > 100 || email.length() > 100) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Name or email too long (max 100 chars)."));
            return;
        }

        if (!email.contains("@")) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Please enter a valid email address."));
            return;
        }

        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Password must be at least 6 characters."));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Check if email taken by another user
            try (PreparedStatement check = con.prepareStatement("SELECT id FROM users WHERE email = ? AND id != ?")) {
                check.setString(1, email);
                check.setInt(2, userId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        JsonUtil.send(res, 409, JsonUtil.message(false, "Email address is already in use by another account."));
                        return;
                    }
                }
            }

            // Execute update
            if (!newPassword.isEmpty()) {
                String sql = "UPDATE users SET name = ?, email = ?, password = ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, PasswordUtil.hash(newPassword));
                    ps.setInt(4, userId);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }
            }

            // Update session
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.setAttribute("name", name);
                session.setAttribute("email", email);
            }

            ActivityLogger.log(con, userId, "UPDATED PROFILE (" + name + ")");

            JsonUtil.send(res, 200, Map.of(
                "success", true,
                "message", "Profile updated successfully.",
                "name", name,
                "email", email
            ));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error updating profile."));
        }
    }
}
