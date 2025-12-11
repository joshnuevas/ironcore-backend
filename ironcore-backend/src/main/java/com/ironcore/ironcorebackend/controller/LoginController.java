package com.ironcore.ironcorebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.LoginRequest;
import com.ironcore.ironcorebackend.entity.LoginResponse;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.util.JwtUtil;

import jakarta.servlet.http.HttpSession;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Utility helper to check if a string is null or blank after trimming.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Handle user login:
     * - Validates input
     * - Checks credentials
     * - Generates JWT
     * - Stores userId in session
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpSession session) {

        // Basic null/blank validation (defense-in-depth; frontend already validates)
        if (request == null ||
                isBlank(request.getEmail()) ||
                isBlank(request.getPassword())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String rawPassword = request.getPassword().trim();

        User user = userRepository.findByEmail(email);

        // Do not reveal whether email exists (OWASP: no user enumeration)
        if (user == null || user.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        }

        // Compare using PasswordEncoder (BCrypt) instead of plain equals
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getUsername());

        // Session-based identification for /api/users/me
        session.setAttribute("userId", user.getId());

        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "Login successful!"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Invalidate the current session (logout).
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }
}
