package com.college.servlet;

import com.college.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/** GET /api/me  ->  who is logged in? (pages use this to protect themselves) */
@WebServlet("/api/me")
public class MeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.send(res, 401, JsonUtil.message(false, "Not logged in."));
            return;
        }
        JsonUtil.send(res, 200, Map.of(
            "success", true,
            "name", session.getAttribute("name"),
            "role", session.getAttribute("role")));
    }
}
