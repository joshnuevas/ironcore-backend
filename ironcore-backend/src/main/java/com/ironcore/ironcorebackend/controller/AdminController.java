package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.TransactionRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

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
            logger.error("Error fetching admin stats", e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch admin statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get enrolled users for a specific schedule
    @GetMapping("/schedules/{scheduleId}/users")
    public ResponseEntity<?> getEnrolledUsers(@PathVariable long scheduleId, HttpSession session) {
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
                            enrollment.isPaid() &&
                            !Boolean.TRUE.equals(enrollment.getSessionCompleted()))
                    .collect(Collectors.toList());

            // Map to user info
            List<Map<String, Object>> enrolledUsers = enrollments.stream()
                    .map(enrollment -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        User user = enrollment.getUser();
                        Transaction transaction = enrollment.getTransaction();

                        userInfo.put("enrollmentId", enrollment.getId());
                        userInfo.put("userId", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("email", user.getEmail());
                        if (transaction != null) {
                            userInfo.put("transactionCode", transaction.getTransactionCode());
                            userInfo.put("paymentDate", transaction.getPaymentDate());
                        } else {
                            userInfo.put("transactionCode", null);
                            userInfo.put("paymentDate", null);
                        }
                        userInfo.put("sessionCompleted", enrollment.getSessionCompleted());

                        return userInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(enrolledUsers);

        } catch (Exception e) {
            logger.error("Error fetching enrolled users for scheduleId {}", scheduleId, e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch enrolled users");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Mark user's session as completed
    @PutMapping("/schedules/{scheduleId}/users/{enrollmentId}/complete")
    public ResponseEntity<?> markSessionCompleted(
            @PathVariable long scheduleId,
            @PathVariable long enrollmentId,
            HttpSession session) {

        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            // Find the enrollment
            ClassEnrollment enrollment = classEnrollmentService.getEnrollmentById(enrollmentId)
                    .orElse(null);
            if (enrollment == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Verify the enrollment belongs to this schedule
            if (enrollment.getSchedule() == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment does not belong to any schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            if (!enrollment.getSchedule().getId().equals(scheduleId)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Enrollment does not belong to this schedule");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Get the schedule
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Schedule not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Check if session is already completed
            if (Boolean.TRUE.equals(enrollment.getSessionCompleted())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Session already marked as completed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Mark session as completed
            enrollment.setSessionCompleted(true);
            classEnrollmentService.saveEnrollment(enrollment);

            // Update enrolled count (decrease by 1) to free up the slot
            if (schedule.getEnrolledCount() > 0) {
                schedule.setEnrolledCount(schedule.getEnrolledCount() - 1);
                scheduleRepository.save(schedule);
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Session marked as completed and slot freed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error marking session as completed. scheduleId={}, enrollmentId={}",
                    scheduleId, enrollmentId, e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark session as completed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
