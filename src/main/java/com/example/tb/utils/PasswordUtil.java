package com.example.tb.utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class PasswordUtil {
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}