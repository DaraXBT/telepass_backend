package com.example.tb.authentication.repository.Admin;

import com.example.tb.model.entity.Admin;
import com.example.tb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Adminrepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
}
