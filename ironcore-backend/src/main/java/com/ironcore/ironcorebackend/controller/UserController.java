package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Create response map with only username and email
            Map<String, String> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
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
        
        // Update username if provided
        if (updates.containsKey("username")) {
            String newUsername = updates.get("username").trim();
            
            // Validate username is not empty
            if (newUsername.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Check if username is already taken by another user
            User existingUser = userRepository.findByUsername(newUsername);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Username is already taken by another user");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            
            user.setUsername(newUsername);
        }
        
        // Update email if provided
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email").trim();
            
            // Validate email is not empty
            if (newEmail.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Basic email format validation
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid email format");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Check if email is already taken by another user
            User existingUser = userRepository.findByEmail(newEmail);
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email is already taken by another user");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            
            user.setEmail(newEmail);
        }
        
        // Save updated user
        userRepository.save(user);
        
        // Return updated user data
        Map<String, String> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("message", "Profile updated successfully");
        
        return ResponseEntity.ok(response);
    }
}