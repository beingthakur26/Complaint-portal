package com.college.util;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small helper to send JSON back to the browser. */
public class JsonUtil {

    private static final Gson GSON = new Gson();

    public static void send(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(GSON.toJson(body));
    }

    /** Builds {"success": true/false, "message": "..."} */
    public static Map<String, Object> message(boolean success, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success);
        map.put("message", message);
        return map;
    }
}
