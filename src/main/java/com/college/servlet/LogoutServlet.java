package com.college.servlet;

import com.college.util.ActivityLogger;
import com.college.util.DBConnection;
import com.college.util.JsonUtil;
import com.college.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/** POST /api/logout  ->  ends the session. */
@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Integer userId = SessionUtil.getUserId(req);

        if (userId != null) {
            try (Connection con = DBConnection.getConnection()) {
                ActivityLogger.log(con, userId, "LOGGED OUT");
            } catch (SQLException e) {
                e.printStackTrace();   // logging failure should not block logout
            }
        }

        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();

        JsonUtil.send(res, 200, JsonUtil.message(true, "Logged out."));
    }
}
