package com.college.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Reads login info from the HTTP session. */
public class SessionUtil {

    /** Returns the logged-in user's id, or null if not logged in. */
    public static Integer getUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);   // false = don't create a new one
        if (session == null) return null;
        return (Integer) session.getAttribute("userId");
    }

    public static boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        return "ADMIN".equals(session.getAttribute("role"));
    }
}
