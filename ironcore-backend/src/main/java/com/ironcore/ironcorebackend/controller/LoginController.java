package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.LoginRequest;
import com.ironcore.ironcorebackend.entity.LoginResponse;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")  // ← Add allowCredentials
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    // Repository to access user data from database
    @Autowired
    private UserRepository userRepository;

    // Encoder to check hashed passwords
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Utility to generate JWT tokens for authentication
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpSession session) {  // ← Add HttpSession parameter
        User user = userRepository.findByEmail(request.getEmail());
        
        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getUsername());
            
            // ⭐ CRITICAL: Store userId in session
            session.setAttribute("userId", user.getId());
            
            // Create response with token and user info
            LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "Login successful!"
            );
            
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid email or password.");
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }
}