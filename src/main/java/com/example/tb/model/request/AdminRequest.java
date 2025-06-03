package com.example.tb.model.request;

import com.example.tb.model.entity.Admin;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({ "username", "password", "email", "profile" })
public class AdminRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String profile;

    public Admin toEntity() {
        Admin admin = new Admin();
        admin.setUsername(this.username);
        admin.setPassword(this.password);
        admin.setEnabled(false);
        admin.setEmail(this.email);
        admin.setProfile(this.profile);
        return admin;
    }
}
