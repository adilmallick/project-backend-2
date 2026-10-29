package com.swiggy.user.service;

import com.swiggy.common.exception.BusinessException;
import com.swiggy.common.exception.ResourceNotFoundException;
import com.swiggy.user.dto.AuthResponse;
import com.swiggy.user.dto.LoginRequest;
import com.swiggy.user.dto.RegisterRequest;
import com.swiggy.user.dto.UserResponse;
import com.swiggy.user.model.User;
import com.swiggy.user.repository.UserRepository;
import com.swiggy.user.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication and User Management Service.
 *
 * <p>DECISION: Encapsulates user registration, password verification, and profile retrieval.
 *
 * <p>INVARIANT: Password comparison and hashing must always use {@link PasswordEncoder}.
 *
 * <p>SEE: ADR-003
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_EXISTS",
                    "A user with this email address already exists",
                    HttpStatus.CONFLICT
            );
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .name(request.name().trim())
                .phone(request.phone().trim())
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);
        long expiresInSeconds = jwtService.getExpirationMinutes() * 60;

        return AuthResponse.of(
                token,
                expiresInSeconds,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // DECISION: Uniform error message for both non-existent email and wrong password
        // to prevent account enumeration attacks.
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "Invalid email or password",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    "Invalid email or password",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String token = jwtService.generateToken(user);
        long expiresInSeconds = jwtService.getExpirationMinutes() * 60;

        return AuthResponse.of(
                token,
                expiresInSeconds,
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.fromEntity(user);
    }
}
