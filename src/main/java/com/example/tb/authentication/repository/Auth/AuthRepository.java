package com.example.tb.authentication.repository.Auth;

import com.example.tb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuthRepository extends JpaRepository<User, Long> {
}
