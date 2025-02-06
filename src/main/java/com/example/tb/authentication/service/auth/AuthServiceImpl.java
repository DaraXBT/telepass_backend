package com.example.tb.authentication.service.auth;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.tb.authentication.repository.Auth.AuthRepository;
import com.example.tb.model.entity.User;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService{
    private  final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    public AuthServiceImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public List<User> getUSer() {
        return authRepository.findAll();
    }

}
