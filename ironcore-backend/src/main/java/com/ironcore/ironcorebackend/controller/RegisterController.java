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

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest().body("Invalid request.");
        }

        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        String securityQuestion = request.getSecurityQuestion();
        String securityAnswer = request.getSecurityAnswer();

        // Basic validation – you can also move this to Bean Validation later
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body("Username, email, and password are required.");
        }

        if (username.length() < 3 || username.length() > 50) {
            return ResponseEntity.badRequest().body("Username must be between 3 and 50 characters.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$") || email.length() > 254) {
            return ResponseEntity.badRequest().body("Invalid email format.");
        }

        if (password.length() < 8 || password.length() > 128) {
            return ResponseEntity.badRequest().body("Password must be between 8 and 128 characters.");
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already exists!");
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already taken!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        // OWASP: store password as BCrypt hash, NOT plain text
        user.setPassword(passwordEncoder.encode(password));

        // You might later want to hash the security answer as well,
        // but that depends on how your ForgotPassword flow is implemented.
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswer(securityAnswer);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully!");
    }
}
