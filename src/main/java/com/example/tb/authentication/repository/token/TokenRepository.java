package com.example.tb.authentication.repository.token;


import com.example.tb.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<VerificationToken, String> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime now);
}
