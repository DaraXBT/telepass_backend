package com.example.tb.authentication.service.admin;

import com.example.tb.authentication.repository.Admin.Adminrepository;
import com.example.tb.model.entity.Admin;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Service
public class Adminservice implements AdminImp {
    private final Adminrepository adminrepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Constructor-based Dependency Injection
    public Adminservice(Adminrepository adminrepository) {
        this.adminrepository = adminrepository;
    }

    @Override
    public Admin login(String email, String password) {
        Optional<Admin> adminOptional = adminrepository.findByEmail(email);

        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();

            // Check if password matches
            if (passwordEncoder.matches(password, admin.getPassword())) {
                System.out.println("✅ Admin login successful!");
                return admin;
            } else {
                System.out.println("❌ Password does not match!");
            }
        } else {
            System.out.println("❌ Admin not found with email: " + email);
        }
        throw new RuntimeException("Invalid email or password");
    }

    // ✅ Initialize Admin on Startup (Creates admin if not exists)
    @PostConstruct
    public void initAdminUser() {
        if (adminrepository.findByEmail("admin@telepass.com").isEmpty()) {
            Admin admin = new Admin();
            admin.setEmail("admin@telepass.com");
            admin.setPassword(passwordEncoder.encode("admin123")); // Hashed password
            adminrepository.save(admin);
            System.out.println("✅ Admin user created successfully.");
        } else {
            System.out.println("ℹ️ Admin user already exists.");
        }
    }
}
