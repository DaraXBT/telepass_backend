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
public class AdminRequest {    @NotBlank(message = "Username is required")
    private String username;

    private String password; // Made optional for Google accounts

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String profile;
    
    // Google OAuth fields
    private String googleId;
    private String fullName;
    private String profileImage;
    private boolean isGoogleAccount = false;
    private boolean enabled = false;    public Admin toEntity() {
        Admin admin = new Admin();
        admin.setUsername(this.username);
        admin.setPassword(this.password);
        admin.setEnabled(this.enabled);
        admin.setEmail(this.email);
        admin.setProfile(this.profile);
        admin.setGoogleId(this.googleId);
        admin.setFullName(this.fullName);
        admin.setProfileImage(this.profileImage);
        admin.setGoogleAccount(this.isGoogleAccount);
        return admin;
    }
}
