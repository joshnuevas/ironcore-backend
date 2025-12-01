package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/auth/forgot-password")
public class ForgotPasswordController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/init")
    public ResponseEntity<?> initForgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email is required."));
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No account found with that email."));
        }

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No security question set for this account."));
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

        if (email == null || email.trim().isEmpty()
                || securityAnswer == null || securityAnswer.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email and security answer are required."));
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No account found with that email."));
        }

        String storedAnswer = user.getSecurityAnswer();
        if (storedAnswer == null ||
                !storedAnswer.trim().equalsIgnoreCase(securityAnswer.trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect security answer."));
        }

        return ResponseEntity.ok(Map.of("message", "Security answer verified."));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String securityAnswer = body.get("securityAnswer");
        String newPassword = body.get("newPassword");

        if (email == null || email.trim().isEmpty()
                || securityAnswer == null || securityAnswer.trim().isEmpty()
                || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email, security answer, and new password are required."));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "New password must be at least 8 characters long."));
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No account found with that email."));
        }

        String storedAnswer = user.getSecurityAnswer();
        if (storedAnswer == null ||
                !storedAnswer.trim().equalsIgnoreCase(securityAnswer.trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect security answer."));
        }

        user.setPassword(newPassword);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "Password reset successfully. You can now log in with your new password.")
        );
    }
}
