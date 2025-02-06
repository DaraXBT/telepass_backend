package com.example.tb.authentication.repository.Auth;

import com.example.tb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
