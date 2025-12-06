package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.AdminService;
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

        return null;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats(HttpSession session) {
        // Verify admin access
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            Map<String, Object> stats = adminService.getAdminStats();
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

            // Get enrolled users for this schedule
            List<Map<String, Object>> enrolledUsers = adminService.getEnrolledUsersForSchedule(scheduleId);
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
            adminService.markSessionAsCompleted(scheduleId, enrollmentId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Session marked as completed and slot freed successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error marking session. scheduleId={}, enrollmentId={}: {}", 
                    scheduleId, enrollmentId, e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error marking session as completed. scheduleId={}, enrollmentId={}",
                    scheduleId, enrollmentId, e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark session as completed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
