package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.Schedule;
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
import java.util.List;
import java.util.stream.Collectors;

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

    // NEW: Get enrolled users for a specific schedule
    @GetMapping("/schedules/{scheduleId}/users")
    public ResponseEntity<?> getEnrolledUsers(@PathVariable Long scheduleId, HttpSession session) {
        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            // Verify schedule exists
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Schedule not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Get all transactions for this schedule with COMPLETED payment status and NOT completed session
            List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getSchedule() != null && 
                           t.getSchedule().getId().equals(scheduleId) &&
                           t.getPaymentStatus() == PaymentStatus.COMPLETED &&
                           !Boolean.TRUE.equals(t.getSessionCompleted()))
                .collect(Collectors.toList());

            // Map to user info
            List<Map<String, Object>> enrolledUsers = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    User user = transaction.getUser();
                    
                    userInfo.put("transactionId", transaction.getId());
                    userInfo.put("userId", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("transactionCode", transaction.getTransactionCode());
                    userInfo.put("paymentDate", transaction.getPaymentDate());
                    userInfo.put("sessionCompleted", transaction.getSessionCompleted());
                    
                    return userInfo;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(enrolledUsers);

        } catch (Exception e) {
            System.err.println("Error fetching enrolled users: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch enrolled users");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // NEW: Mark user's session as completed
    @PutMapping("/schedules/{scheduleId}/users/{transactionId}/complete")
    @ResponseBody
    public ResponseEntity<?> markSessionCompleted(
            @PathVariable Long scheduleId,
            @PathVariable Long transactionId,
            HttpSession session) {
        
        System.out.println("=== Mark Session Completed Request ===");
        System.out.println("Schedule ID: " + scheduleId);
        System.out.println("Transaction ID: " + transactionId);
        
        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            System.out.println("Admin access check failed");
            return accessCheck;
        }
        
        System.out.println("Admin access verified");

        try {
            // Find the transaction
            Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
            if (transaction == null) {
                System.out.println("Transaction not found: " + transactionId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Transaction not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            System.out.println("Transaction found: " + transaction.getTransactionCode());

            // Verify the transaction belongs to this schedule
            if (transaction.getSchedule() == null) {
                System.out.println("Transaction has no schedule");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Transaction does not belong to any schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            if (!transaction.getSchedule().getId().equals(scheduleId)) {
                System.out.println("Transaction schedule ID mismatch: " + transaction.getSchedule().getId() + " vs " + scheduleId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Transaction does not belong to this schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            System.out.println("Transaction belongs to schedule: " + scheduleId);

            // Get the schedule
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                System.out.println("Schedule not found: " + scheduleId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Schedule not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            System.out.println("Schedule found. Current enrolled count: " + schedule.getEnrolledCount());

            // Check if session is already completed
            if (Boolean.TRUE.equals(transaction.getSessionCompleted())) {
                System.out.println("Session already completed");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Session already marked as completed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Mark session as completed
            transaction.setSessionCompleted(true);
            transactionRepository.save(transaction);
            System.out.println("Transaction marked as completed");

            // Update enrolled count (decrease by 1) to free up the slot
            if (schedule.getEnrolledCount() > 0) {
                schedule.setEnrolledCount(schedule.getEnrolledCount() - 1);
                scheduleRepository.save(schedule);
                System.out.println("Schedule enrolled count updated to: " + schedule.getEnrolledCount());
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Session marked as completed and slot freed successfully");
            System.out.println("=== Success ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error marking session as completed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark session as completed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}