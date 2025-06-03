package com.example.tb.authentication.repository.admin;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.tb.model.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
    UserDetails findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT a FROM Admin a WHERE a.username = :username")
    Admin findByUsernameReturnAuth(@Param("username") String username);

    @Query("SELECT a FROM Admin a WHERE a.email = :email")
    Admin findUserByEmail(@Param("email") String email);
}
