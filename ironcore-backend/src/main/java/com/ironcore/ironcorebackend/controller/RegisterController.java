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

import com.ironcore.ironcorebackend.entity.RegisterRequest;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/auth")
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Helper to check if a string is null or empty after trimming.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Helper to build a simple error response with a message.
     */
    private ResponseEntity<String> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {

        if (request == null) {
            return error(HttpStatus.BAD_REQUEST, "Invalid request.");
        }

        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        String securityQuestion = request.getSecurityQuestion();
        String securityAnswer = request.getSecurityAnswer();

        // Basic validation – you can also move this to Bean Validation later
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Username, email, and password are required.");
        }

        if (username.length() < 3 || username.length() > 50) {
            return error(HttpStatus.BAD_REQUEST, "Username must be between 3 and 50 characters.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$") || email.length() > 254) {
            return error(HttpStatus.BAD_REQUEST, "Invalid email format.");
        }

        if (password.length() < 8 || password.length() > 128) {
            return error(HttpStatus.BAD_REQUEST, "Password must be between 8 and 128 characters.");
        }

        if (userRepository.existsByEmail(email)) {
            return error(HttpStatus.CONFLICT, "Email already exists!");
        }

        if (userRepository.existsByUsername(username)) {
            return error(HttpStatus.CONFLICT, "Username already taken!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        // OWASP: store password as BCrypt hash, NOT plain text
        user.setPassword(passwordEncoder.encode(password));

        // Security question/answer stored as-is (same behavior as before)
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(securityAnswer);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully!");
    }
}
