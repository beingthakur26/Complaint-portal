package com.college.servlet;

import com.college.util.ActivityLogger;
import com.college.util.DBConnection;
import com.college.util.JsonUtil;
import com.college.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET  /api/complaints  ->  list MY complaints
 * POST /api/complaints  ->  submit a new complaint
 */
@WebServlet("/api/complaints")
public class ComplaintServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Integer userId = SessionUtil.getUserId(req);
        if (userId == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Please log in."));
            return;
        }

        String sql = "SELECT id, title, description, status, created_at FROM complaints "
                   + "WHERE user_id = ? ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);   // only THIS user's complaints
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("title", rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("status", rs.getString("status"));
                    row.put("createdAt", rs.getString("created_at"));
                    list.add(row);
                }
            }
            JsonUtil.send(res, 200, list);

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
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

        String title = req.getParameter("title");
        String description = req.getParameter("description");
        title = title == null ? "" : title.trim();
        description = description == null ? "" : description.trim();

        if (title.isEmpty() || description.isEmpty()) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Title and description are required."));
            return;
        }
        if (title.length() > 150) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Title must be 150 characters or less."));
            return;
        }

        String sql = "INSERT INTO complaints (user_id, title, description) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.executeUpdate();

            int complaintId;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                complaintId = keys.getInt(1);
            }

            ActivityLogger.log(con, userId, "SUBMITTED COMPLAINT #" + complaintId);
            JsonUtil.send(res, 200, JsonUtil.message(true, "Complaint submitted."));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
        }
    }
}
