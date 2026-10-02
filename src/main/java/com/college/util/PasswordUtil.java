package com.college.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Turns a password into a SHA-256 hash so we never store plain text. */
public class PasswordUtil {

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);   // 64-character hex string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
