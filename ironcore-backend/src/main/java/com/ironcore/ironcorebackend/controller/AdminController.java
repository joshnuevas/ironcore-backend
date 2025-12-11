package com.ironcore.ironcorebackend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.AdminService;

import jakarta.servlet.http.HttpSession;

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
    private AdminService adminService;

    /**
     * Helper to build a standard error response body.
     */
    private ResponseEntity<Map<String, String>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Helper method to check if the current session belongs to an authenticated admin user.
     * Returns a ResponseEntity with an error if not allowed, or null if access is granted.
     */
    private ResponseEntity<Map<String, String>> verifyAdminAccess(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return buildErrorResponse("Not authenticated. Please log in.", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return buildErrorResponse("User not found", HttpStatus.NOT_FOUND);
        }

        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            return buildErrorResponse("Access denied. Admin privileges required.", HttpStatus.FORBIDDEN);
        }

        // null means access granted
        return null;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats(HttpSession session) {
        // Verify admin access
        ResponseEntity<Map<String, String>> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            return accessCheck;
        }

        try {
            Map<String, Object> stats = adminService.getAdminStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching admin stats", e);
            return buildErrorResponse("Failed to fetch admin statistics",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get enrolled users for a specific schedule.
     */
    @GetMapping("/schedules/{scheduleId}/users")
    public ResponseEntity<?> getEnrolledUsers(@PathVariable long scheduleId, HttpSession session) {
        // Verify admin access
        ResponseEntity<Map<String, String>> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            return accessCheck;
        }

        try {
            // Verify schedule exists
            Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
            if (schedule == null) {
                return buildErrorResponse("Schedule not found", HttpStatus.NOT_FOUND);
            }

            // Get enrolled users for this schedule
            List<Map<String, Object>> enrolledUsers =
                    adminService.getEnrolledUsersForSchedule(scheduleId);

            return ResponseEntity.ok(enrolledUsers);
        } catch (Exception e) {
            logger.error("Error fetching enrolled users for scheduleId {}", scheduleId, e);
            return buildErrorResponse("Failed to fetch enrolled users",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Mark user's session as completed.
     */
    @PutMapping("/schedules/{scheduleId}/users/{enrollmentId}/complete")
    public ResponseEntity<?> markSessionCompleted(
            @PathVariable long scheduleId,
            @PathVariable long enrollmentId,
            HttpSession session
    ) {
        // Verify admin access
        ResponseEntity<Map<String, String>> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            return accessCheck;
        }

        try {
            adminService.markSessionAsCompleted(scheduleId, enrollmentId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Session marked as completed and slot freed successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Validation error marking session. scheduleId={}, enrollmentId={}: {}",
                    scheduleId, enrollmentId, e.getMessage()
            );

            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error(
                    "Error marking session as completed. scheduleId={}, enrollmentId={}",
                    scheduleId, enrollmentId, e
            );

            return buildErrorResponse(
                    "Failed to mark session as completed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Cancel a membership by transaction code.
     */
    @PutMapping("/memberships/{transactionCode}/cancel")
    public ResponseEntity<?> cancelMembership(
            @PathVariable String transactionCode,
            HttpSession session
    ) {
        // Verify admin access
        ResponseEntity<Map<String, String>> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) {
            return accessCheck;
        }

        try {
            adminService.cancelMembership(transactionCode);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Membership has been cancelled successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Validation error cancelling membership. transactionCode={}: {}",
                    transactionCode, e.getMessage()
            );

            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error cancelling membership. transactionCode={}", transactionCode, e);
            return buildErrorResponse(
                    "Failed to cancel membership: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
