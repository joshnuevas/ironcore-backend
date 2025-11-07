package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Get current logged-in user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        
        if (userId == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Not authenticated. Please log in.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (!userOptional.isPresent()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        User user = userOptional.get();
        
        // Return user data including admin status
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        // Ensure boolean value is returned properly
        response.put("isAdmin", Boolean.TRUE.equals(user.getIsAdmin()));
        
        if (user.getProfilePicture() != null && user.getProfilePicture().length > 0) {
            String base64Image = Base64.getEncoder().encodeToString(user.getProfilePicture());
            String mimeType = user.getProfilePictureMimeType() != null ? 
                user.getProfilePictureMimeType() : "image/jpeg";
            response.put("profilePicture", "data:" + mimeType + ";base64," + base64Image);
        } else {
            response.put("profilePicture", "");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("isAdmin", user.getIsAdmin() != null ? user.getIsAdmin() : false);
            
            if (user.getProfilePicture() != null && user.getProfilePicture().length > 0) {
                String base64Image = Base64.getEncoder().encodeToString(user.getProfilePicture());
                String mimeType = user.getProfilePictureMimeType() != null ? 
                    user.getProfilePictureMimeType() : "image/jpeg";
                response.put("profilePicture", "data:" + mimeType + ";base64," + base64Image);
            } else {
                response.put("profilePicture", "");
            }
            
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found");
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> updates) {
        
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (!userOptional.isPresent()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        User user = userOptional.get();
        
        if (updates.containsKey("username")) {
            String newUsername = updates.get("username").trim();
            
            if (newUsername.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            User existingUser = userRepository.findByUsername(newUsername);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username is already taken by another user");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            
            user.setUsername(newUsername);
        }
        
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email").trim();
            
            if (newEmail.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid email format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            User existingUser = userRepository.findByEmail(newEmail);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email is already taken by another user");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            
            user.setEmail(newEmail);
        }
        
        userRepository.save(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("message", "Profile updated successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("profilePicture") MultipartFile file) {
        
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (!userOptional.isPresent()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        if (file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Please select a file to upload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Only image files are allowed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "File size should not exceed 5MB");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        try {
            byte[] imageBytes = file.getBytes();
            
            User user = userOptional.get();
            user.setProfilePicture(imageBytes);
            user.setProfilePictureMimeType(contentType);
            userRepository.save(user);
            
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + contentType + ";base64," + base64Image;
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile picture uploaded successfully");
            response.put("profilePictureUrl", dataUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{userId}/profile-picture")
    public ResponseEntity<?> deleteProfilePicture(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (!userOptional.isPresent()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        User user = userOptional.get();
        user.setProfilePicture(null);
        user.setProfilePictureMimeType(null);
        userRepository.save(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile picture deleted successfully");
        
        return ResponseEntity.ok(response);
    }
}