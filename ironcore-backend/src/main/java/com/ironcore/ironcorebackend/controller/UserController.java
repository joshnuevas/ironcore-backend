package com.ironcore.ironcorebackend.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ========================================
    // Helpers
    // ========================================

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("message", msg));
    }

    private ResponseEntity<Map<String, String>> badRequest(String msg) {
        return message(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, String>> notFound(String msg) {
        return message(HttpStatus.NOT_FOUND, msg);
    }

    private User getUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String buildProfilePictureDataUri(User user) {
        if (user.getProfilePicture() == null || user.getProfilePicture().length == 0) {
            return "";
        }
        String base64Image = Base64.getEncoder().encodeToString(user.getProfilePicture());
        String mimeType = user.getProfilePictureMimeType() != null
                ? user.getProfilePictureMimeType()
                : "image/jpeg";
        return "data:" + mimeType + ";base64," + base64Image;
    }

    private Map<String, Object> buildUserResponse(User user, boolean includeId) {
        Map<String, Object> response = new HashMap<>();
        if (includeId) {
            response.put("id", user.getId());
        }
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()));
        response.put("profilePicture", buildProfilePictureDataUri(user));
        return response;
    }

    // ========================================
    // Current user
    // ========================================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return message(HttpStatus.UNAUTHORIZED, "Not authenticated. Please log in.");
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return notFound("User not found");
        }

        User user = userOptional.get();
        Map<String, Object> response = buildUserResponse(user, true);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // Get user by ID
    // ========================================

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable long userId) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return notFound("User not found");
        }

        User user = userOptional.get();
        Map<String, Object> response = buildUserResponse(user, false);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // Update user profile (username/email)
    // ========================================

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable long userId,
                                        @RequestBody Map<String, String> updates) {

        User user = getUserOrThrow(userId);

        if (updates.containsKey("username")) {
            String newUsername = updates.get("username").trim();
            if (newUsername.isEmpty()) {
                return badRequest("Username cannot be empty");
            }

            User existingUser = userRepository.findByUsername(newUsername);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Username already taken"));
            }

            user.setUsername(newUsername);
        }

        if (updates.containsKey("email")) {
            String newEmail = updates.get("email").trim();
            if (newEmail.isEmpty()) {
                return badRequest("Email cannot be empty");
            }

            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return badRequest("Invalid email format");
            }

            User existing = userRepository.findByEmail(newEmail);
            if (existing != null && !existing.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Email already taken"));
            }

            user.setEmail(newEmail);
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    // ========================================
    // Change password
    // ========================================

    @PutMapping("/{userId}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable long userId,
                                            @RequestBody Map<String, String> body) {

        User user = getUserOrThrow(userId);

        String current = body.getOrDefault("currentPassword", "").trim();
        String next = body.getOrDefault("newPassword", "").trim();
        String confirm = body.getOrDefault("confirmPassword", "").trim();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            return badRequest("All fields required");
        }

        // Compare current password using BCrypt
        if (!passwordEncoder.matches(current, user.getPassword())) {
            return badRequest("Current password incorrect");
        }

        if (!next.equals(confirm)) {
            return badRequest("Passwords do not match");
        }

        if (next.equals(current)) {
            return badRequest("New password must be different");
        }

        if (next.length() < 8 || next.length() > 128) {
            return badRequest("Password must be between 8 and 128 characters.");
        }

        // Store new password as BCrypt hash
        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // ========================================
    // Profile picture: upload
    // ========================================

    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable long userId,
                                                  @RequestParam("profilePicture") MultipartFile file) {

        Optional<User> optional = userRepository.findById(userId);
        if (optional.isEmpty()) {
            return notFound("User not found");
        }

        if (file.isEmpty()) {
            return badRequest("No file uploaded");
        }

        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) {
            return badRequest("Invalid file type");
        }

        try {
            byte[] bytes = file.getBytes();
            User user = optional.get();

            user.setProfilePicture(bytes);
            user.setProfilePictureMimeType(type);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Image updated"));
        } catch (IOException e) {
            return message(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
        }
    }

    // ========================================
    // Profile picture: delete
    // ========================================

    @DeleteMapping("/{userId}/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable long userId) {
        Optional<User> optional = userRepository.findById(userId);

        if (optional.isEmpty()) {
            return notFound("User not found");
        }

        User user = optional.get();
        user.setProfilePicture(null);
        user.setProfilePictureMimeType(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile picture removed"));
    }
}
