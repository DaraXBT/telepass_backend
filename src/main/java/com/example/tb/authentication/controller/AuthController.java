package com.example.tb.authentication.controller;

import com.example.tb.authentication.service.auth.AuthService;
import com.example.tb.model.entity.User;
import com.example.tb.model.response.ApiResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private  final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/getUSer")
    public ApiResponse<?> getUSer() {
        List<User> users = authService.getUSer();
        return ApiResponse.builder()
                .payload(users)
                .date(LocalDateTime.now())
                .message("successfully change password")
                .build();
    }

    @GetMapping("/test")
            public String test () {
        return "Hello World";
            }

}
