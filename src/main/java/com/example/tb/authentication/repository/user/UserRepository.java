package com.example.tb.authentication.repository.user;

import com.example.tb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Check if phone number already exists
    boolean existsByPhoneNumber(String phoneNumber);

    // Find user by registration token
    Optional<User> findByRegistrationToken(String token);

    // Find user by phone number
    Optional<User> findByPhoneNumber(String phoneNumber);
}
