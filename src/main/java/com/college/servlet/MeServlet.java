package com.college.servlet;

import com.college.util.DBConnection;
import com.college.util.JsonUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/me  ->  who is logged in? (pages use this to protect themselves & populate profile) */
@WebServlet("/api/me")
public class MeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Not logged in."));
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        String name = (String) session.getAttribute("name");
        String role = (String) session.getAttribute("role");
        String email = (String) session.getAttribute("email");

        // If email not cached in session, fetch from DB
        if (email == null || email.isBlank()) {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT name, email, role FROM users WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString("name");
                        email = rs.getString("email");
                        role = rs.getString("role");
                        session.setAttribute("name", name);
                        session.setAttribute("email", email);
                        session.setAttribute("role", role);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("id", userId);
        data.put("name", name != null ? name : "User");
        data.put("email", email != null ? email : "");
        data.put("role", role != null ? role : "USER");

        JsonUtil.send(res, 200, data);
    }
}
