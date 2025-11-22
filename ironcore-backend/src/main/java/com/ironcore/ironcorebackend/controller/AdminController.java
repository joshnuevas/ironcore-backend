package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
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

    @Autowired
    private ClassEnrollmentService classEnrollmentService;

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
        
        // Count active schedules - use repository directly
        long activeSchedules = scheduleRepository.count();
        
        // Count total registered users (excluding admins) - more efficient query
        long totalMembers = userRepository.countByIsAdminFalseOrIsAdminIsNull();
        
        // If the above method doesn't exist, use this alternative:
        // long totalMembers = userRepository.findNonAdminUsersCount();
        
        // Calculate available slots - more efficient approach
        Integer availableSlotsSum = scheduleRepository.sumAvailableSlots();
        int availableSlots = availableSlotsSum != null ? availableSlotsSum : 0;
        
        // Alternative if custom query method doesn't exist:
        // List<Schedule> allSchedules = scheduleRepository.findAll();
        // int availableSlots = allSchedules.stream()
        //     .mapToInt(schedule -> {
        //         int max = schedule.getMaxParticipants() != null ? schedule.getMaxParticipants() : 0;
        //         int enrolled = schedule.getEnrolledCount() != null ? schedule.getEnrolledCount() : 0;
        //         return Math.max(0, max - enrolled);
        //     })
        //     .sum();
        
        // Count completed transactions
        long completedTransactions = transactionRepository.countByPaymentStatus(PaymentStatus.COMPLETED);
        
        // Debug logging
        System.out.println("=== Admin Stats ===");
        System.out.println("Active Schedules: " + activeSchedules);
        System.out.println("Total Members: " + totalMembers);
        System.out.println("Available Slots: " + availableSlots);
        System.out.println("Completed Transactions: " + completedTransactions);
        
        stats.put("activeSchedules", activeSchedules);
        stats.put("totalMembers", totalMembers);
        stats.put("availableSlots", availableSlots);
        stats.put("completedTransactions", completedTransactions);
        
        return ResponseEntity.ok(stats);
    } catch (Exception e) {
        System.err.println("Error fetching admin stats: " + e.getMessage());
        e.printStackTrace();
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Failed to fetch admin statistics: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

    // NEW: Get enrolled users for a specific schedule - FIXED to use ClassEnrollment
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

            // Get all class enrollments for this schedule with COMPLETED payment status and NOT completed session
            List<ClassEnrollment> enrollments = classEnrollmentService.getAllEnrollments().stream()
                .filter(enrollment -> 
                    enrollment.getSchedule() != null && 
                    enrollment.getSchedule().getId().equals(scheduleId) &&
                    enrollment.isPaid() && // Use the helper method from ClassEnrollment
                    !Boolean.TRUE.equals(enrollment.getSessionCompleted()))
                .collect(Collectors.toList());

            // Map to user info
            List<Map<String, Object>> enrolledUsers = enrollments.stream()
                .map(enrollment -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    User user = enrollment.getUser();
                    
                    userInfo.put("enrollmentId", enrollment.getId());
                    userInfo.put("userId", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("transactionCode", enrollment.getTransaction().getTransactionCode());
                    userInfo.put("paymentDate", enrollment.getTransaction().getPaymentDate());
                    userInfo.put("sessionCompleted", enrollment.getSessionCompleted());
                    
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

    // NEW: Mark user's session as completed - FIXED to use ClassEnrollment
    @PutMapping("/schedules/{scheduleId}/users/{enrollmentId}/complete")
    @ResponseBody
    public ResponseEntity<?> markSessionCompleted(
            @PathVariable Long scheduleId,
            @PathVariable Long enrollmentId,
            HttpSession session) {
        
        System.out.println("=== Mark Session Completed Request ===");
        System.out.println("Schedule ID: " + scheduleId);
        System.out.println("Enrollment ID: " + enrollmentId);
        
        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            System.out.println("Admin access check failed");
            return accessCheck;
        }
        
        System.out.println("Admin access verified");

        try {
            // Find the enrollment
            ClassEnrollment enrollment = classEnrollmentService.getEnrollmentById(enrollmentId)
                .orElse(null);
            if (enrollment == null) {
                System.out.println("Enrollment not found: " + enrollmentId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            System.out.println("Enrollment found for user: " + enrollment.getUser().getUsername());

            // Verify the enrollment belongs to this schedule
            if (enrollment.getSchedule() == null) {
                System.out.println("Enrollment has no schedule");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment does not belong to any schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            if (!enrollment.getSchedule().getId().equals(scheduleId)) {
                System.out.println("Enrollment schedule ID mismatch: " + enrollment.getSchedule().getId() + " vs " + scheduleId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment does not belong to this schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            System.out.println("Enrollment belongs to schedule: " + scheduleId);

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
            if (Boolean.TRUE.equals(enrollment.getSessionCompleted())) {
                System.out.println("Session already completed");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Session already marked as completed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Mark session as completed
            enrollment.setSessionCompleted(true);
            classEnrollmentService.saveEnrollment(enrollment);
            System.out.println("Enrollment marked as completed");

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