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
import java.util.Set;

/**
 * ADMIN ONLY.
 * GET  /api/admin  ->  summary counts + all users + all complaints + activity log
 * POST /api/admin  ->  change a complaint's status
 */
@WebServlet("/api/admin")
public class AdminServlet extends HttpServlet {

    private static final Set<String> VALID_STATUS = Set.of("PENDING", "IN PROGRESS", "RESOLVED");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (!checkAdmin(req, res)) return;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            Map<String, Object> result = new LinkedHashMap<>();

            // 1. Summary counts
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalUsers", count(st, "SELECT COUNT(*) FROM users WHERE role = 'USER'"));
            summary.put("totalComplaints", count(st, "SELECT COUNT(*) FROM complaints"));
            summary.put("pendingComplaints", count(st, "SELECT COUNT(*) FROM complaints WHERE status = 'PENDING'"));
            result.put("summary", summary);

            // 2. All users (never send the password column)
            List<Map<String, Object>> users = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT u.id, u.name, u.email, u.role, "
                  + "(SELECT COUNT(*) FROM complaints c WHERE c.user_id = u.id) AS complaint_count "
                  + "FROM users u ORDER BY u.id")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("name", rs.getString("name"));
                    row.put("email", rs.getString("email"));
                    row.put("role", rs.getString("role"));
                    row.put("complaintCount", rs.getInt("complaint_count"));
                    users.add(row);
                }
            }
            result.put("users", users);

            // 3. All complaints with the user's name
            List<Map<String, Object>> complaints = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT c.id, u.name AS user_name, c.title, c.description, c.status, c.created_at "
                  + "FROM complaints c JOIN users u ON c.user_id = u.id ORDER BY c.id DESC")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("userName", rs.getString("user_name"));
                    row.put("title", rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("status", rs.getString("status"));
                    row.put("createdAt", rs.getString("created_at"));
                    complaints.add(row);
                }
            }
            result.put("complaints", complaints);

            // 4. Latest 100 activity log entries
            List<Map<String, Object>> logs = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT a.id, u.name AS user_name, a.action, a.created_at "
                  + "FROM activity_log a LEFT JOIN users u ON a.user_id = u.id "
                  + "ORDER BY a.id DESC LIMIT 100")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("userName", rs.getString("user_name"));
                    row.put("action", rs.getString("action"));
                    row.put("createdAt", rs.getString("created_at"));
                    logs.add(row);
                }
            }
            result.put("logs", logs);

            JsonUtil.send(res, 200, result);

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if (!checkAdmin(req, res)) return;

        String idText = req.getParameter("complaintId");
        String status = req.getParameter("status");

        int complaintId;
        try {
            complaintId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Invalid complaint id."));
            return;
        }
        if (status == null || !VALID_STATUS.contains(status)) {
            JsonUtil.send(res, 400, JsonUtil.message(false, "Invalid status."));
            return;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE complaints SET status = ? WHERE id = ?")) {

            ps.setString(1, status);
            ps.setInt(2, complaintId);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                JsonUtil.send(res, 404, JsonUtil.message(false, "Complaint not found."));
                return;
            }

            ActivityLogger.log(con, SessionUtil.getUserId(req),
                    "CHANGED COMPLAINT #" + complaintId + " TO " + status);
            JsonUtil.send(res, 200, JsonUtil.message(true, "Status updated."));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.send(res, 500, JsonUtil.message(false, "Database error. Check the Tomcat log."));
        }
    }

    /** Server-side security check: 401 if not logged in, 403 if logged in but not admin. */
    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (SessionUtil.getUserId(req) == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Please log in."));
            return false;
        }
        if (!SessionUtil.isAdmin(req)) {
            JsonUtil.send(res, 403, JsonUtil.message(false, "Admins only."));
            return false;
        }
        return true;
    }

    private int count(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
