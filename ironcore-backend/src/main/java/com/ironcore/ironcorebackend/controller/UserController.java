package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated. Please log in."));
        }

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        User user = userOptional.get();
        Map<String, Object> response = new HashMap<>();

        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()));

        if (user.getProfilePicture() != null && user.getProfilePicture().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(user.getProfilePicture());
            String mimeType = user.getProfilePictureMimeType() != null
                    ? user.getProfilePictureMimeType() : "image/jpeg";
            response.put("profilePicture", "data:" + mimeType + ";base64," + base64Image);
        } else {
            response.put("profilePicture", "");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable long userId) {

        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        User user = userOptional.get();
        Map<String, Object> response = new HashMap<>();

        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()));

        if (user.getProfilePicture() != null && user.getProfilePicture().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(user.getProfilePicture());
            String mimeType = user.getProfilePictureMimeType() != null
                    ? user.getProfilePictureMimeType() : "image/jpeg";
            response.put("profilePicture", "data:" + mimeType + ";base64," + base64Image);
        } else {
            response.put("profilePicture", "");
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable long userId,
                                        @RequestBody Map<String, String> updates) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (updates.containsKey("username")) {
            String newUsername = updates.get("username").trim();
            if (newUsername.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Username cannot be empty"));

            User existingUser = userRepository.findByUsername(newUsername);
            if (existingUser != null && !existingUser.getId().equals(userId))
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username already taken"));

            user.setUsername(newUsername);
        }

        if (updates.containsKey("email")) {
            String newEmail = updates.get("email").trim();
            if (newEmail.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Email cannot be empty"));

            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$"))
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid email format"));

            User existing = userRepository.findByEmail(newEmail);
            if (existing != null && !existing.getId().equals(userId))
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already taken"));

            user.setEmail(newEmail);
        }

        userRepository.save(Objects.requireNonNull(user));
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PutMapping("/{userId}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable long userId,
                                            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String current = body.getOrDefault("currentPassword", "").trim();
        String next = body.getOrDefault("newPassword", "").trim();
        String confirm = body.getOrDefault("confirmPassword", "").trim();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "All fields required"));

        if (!current.equals(user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("message", "Current password incorrect"));

        if (!next.equals(confirm))
            return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));

        if (next.equals(current))
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be different"));

        user.setPassword(next);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable long userId,
                                                  @RequestParam("profilePicture") MultipartFile file) {

        Optional<User> optional = userRepository.findById(userId);

        if (optional.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User not found"));

        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded"));

        String type = file.getContentType();

        if (type == null || !type.startsWith("image/"))
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid file type"));

        try {
            byte[] bytes = file.getBytes();
            User user = optional.get();

            user.setProfilePicture(bytes);
            user.setProfilePictureMimeType(type);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Image updated"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Upload failed"));
        }
    }

    @DeleteMapping("/{userId}/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable long userId) {

        Optional<User> optional = userRepository.findById(userId);

        if (optional.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));

        User user = optional.get();
        user.setProfilePicture(null);
        user.setProfilePictureMimeType(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile picture removed"));
    }
}
