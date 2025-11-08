package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.TransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Helper method to check if user is admin
    private ResponseEntity<?> verifyAdminAccess(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        
        if (userId == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Not authenticated. Please log in.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        // Check if user is admin
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Access denied. Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        return null; // No error, user is admin
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats(HttpSession session) {
        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Count active schedules (all schedules in the system)
            long activeSchedules = scheduleRepository.count();
            
            // Count total registered users (excluding admins)
            long totalMembers = userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsAdmin()))
                .count();
            
            // Calculate available slots (sum of remaining capacity across all schedules)
            int availableSlots = scheduleRepository.findAll().stream()
                .mapToInt(schedule -> schedule.getMaxParticipants() - schedule.getEnrolledCount())
                .sum();
            
            // Count completed transactions (transactions with COMPLETED payment status)
            long completedTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();
            
            stats.put("activeSchedules", activeSchedules);
            stats.put("totalMembers", totalMembers);
            stats.put("availableSlots", availableSlots);
            stats.put("completedTransactions", completedTransactions);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching admin stats: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch admin statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}