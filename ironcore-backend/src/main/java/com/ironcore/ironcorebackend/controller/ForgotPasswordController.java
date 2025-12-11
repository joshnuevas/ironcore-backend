package com.ironcore.ironcorebackend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/auth/forgot-password")
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Utility helper to check if a string is null or blank after trimming.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Utility helper to build a simple JSON { "message": "..." } response.
     */
    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }

    @PostMapping("/init")
    public ResponseEntity<?> initForgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (isBlank(email)) {
            return message(HttpStatus.BAD_REQUEST, "Email is required.");
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return message(HttpStatus.NOT_FOUND, "No account found with that email.");
        }

        if (isBlank(user.getSecurityQuestion())) {
            return message(HttpStatus.BAD_REQUEST, "No security question set for this account.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("securityQuestion", user.getSecurityQuestion());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifySecurityAnswer(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String securityAnswer = body.get("securityAnswer");

        if (isBlank(email) || isBlank(securityAnswer)) {
            return message(HttpStatus.BAD_REQUEST, "Email and security answer are required.");
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return message(HttpStatus.NOT_FOUND, "No account found with that email.");
        }

        String storedAnswer = user.getSecurityAnswer();
        if (storedAnswer == null ||
                !storedAnswer.trim().equalsIgnoreCase(securityAnswer.trim())) {
            return message(HttpStatus.UNAUTHORIZED, "Incorrect security answer.");
        }

        return ResponseEntity.ok(Map.of("message", "Security answer verified."));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String securityAnswer = body.get("securityAnswer");
        String newPassword = body.get("newPassword");

        if (isBlank(email) || isBlank(securityAnswer) || isBlank(newPassword)) {
            return message(
                    HttpStatus.BAD_REQUEST,
                    "Email, security answer, and new password are required."
            );
        }

        if (newPassword.length() < 8) {
            return message(
                    HttpStatus.BAD_REQUEST,
                    "New password must be at least 8 characters long."
            );
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return message(HttpStatus.NOT_FOUND, "No account found with that email.");
        }

        String storedAnswer = user.getSecurityAnswer();
        if (storedAnswer == null ||
                !storedAnswer.trim().equalsIgnoreCase(securityAnswer.trim())) {
            return message(HttpStatus.UNAUTHORIZED, "Incorrect security answer.");
        }

        // NOTE: Password is being set directly here as in your original logic.
        // Hashing/encoding would normally be done in a service layer.
        user.setPassword(newPassword);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "Password reset successfully. You can now log in with your new password.")
        );
    }
}
