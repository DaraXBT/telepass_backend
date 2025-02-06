package com.example.tb.authentication.controller;

import com.example.tb.authentication.service.admin.Adminservice;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.response.ApiResponse;

import com.example.tb.model.response.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {
    private final Adminservice adminservice;

    public LoginController(Adminservice adminservice) {
        this.adminservice = adminservice;
    }

    // ✅ Admin Login API
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody LoginRequest loginRequest) {
        try {
            Admin admin = adminservice.login(loginRequest.getEmail(), loginRequest.getPassword());
            return ResponseEntity.ok(ApiResponse.builder()
                    .payload(admin)
                    .message("Admin Login successful")
                    .date(LocalDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.builder()
                    .message("Invalid email or password")
                    .date(LocalDateTime.now())
                    .build());
        }
    }
}
