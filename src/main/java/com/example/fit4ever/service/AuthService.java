package com.example.fit4ever.service;

import com.example.fit4ever.dto.*;
import com.example.fit4ever.exception.InvalidCredentialsException;
import com.example.fit4ever.exception.UserAlreadyExistsException;
import com.example.fit4ever.model.User;
import com.example.fit4ever.repository.UserRepository;
import com.example.fit4ever.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("An account with this email already exists");
        }
        
        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .emailVerified(false)
                .role("USER")
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        return new AuthResponse(jwtUtil.generateToken(savedUser.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        log.info("User logged in successfully: {}", user.getEmail());
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }
}
