package com.college.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/** Writes one row into activity_log (who did what). */
public class ActivityLogger {

    public static void log(Connection con, Integer userId, String action) throws SQLException {
        String sql = "INSERT INTO activity_log (user_id, action) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, action);
            ps.executeUpdate();
        }
    }
}
