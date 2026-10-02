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
import java.util.Map;

/** POST /api/login  ->  checks email + password, starts a session. */
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        if (email == null || password == null || email.trim().isEmpty() || password.isEmpty()) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Email and password are required."));
            return;
        }
        email = email.trim().toLowerCase();

        String sql = "SELECT id, name, role FROM users WHERE email = ? AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, PasswordUtil.hash(password));   // compare hash with stored hash

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JsonUtil.send(res, 401, JsonUtil.message(false, "Wrong email or password."));
                    return;
                }

                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");

                // Start a fresh session (drop any old one first)
                HttpSession old = req.getSession(false);
                if (old != null) old.invalidate();
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", id);
                session.setAttribute("name", name);
                session.setAttribute("role", role);

                ActivityLogger.log(con, id, "LOGGED IN");
                JsonUtil.send(res, 200, Map.of("success", true, "role", role, "name", name));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
        }
    }
}
