package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceService attendanceService;

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

    // Get active members with valid memberships for today - FIXED
    @GetMapping("/active-members")
    public ResponseEntity<?> getActiveMembers(@RequestParam(required = false) String date, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            List<Map<String, Object>> members = attendanceService.getActiveMembersForDate(targetDate);
            return ResponseEntity.ok(members);

        } catch (RuntimeException e) {
            logger.error("Error in getActiveMembers", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch active members: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            long userId = Long.parseLong(request.get("userId").toString());
            String dateStr = request.get("date").toString();
            Boolean checkedIn = Boolean.valueOf(request.get("checkedIn").toString());

            LocalDate attendanceDate = LocalDate.parse(dateStr);
            Long adminIdObj = (Long) session.getAttribute("userId");
            if (adminIdObj == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated. Please log in."));
            }

            Map<String, Object> response = attendanceService.markAttendance(userId, attendanceDate, checkedIn, adminIdObj);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error marking attendance: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Duplicate attendance record detected", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Attendance record already exists for this user on the selected date");
            errorResponse.put("code", "DUPLICATE_ATTENDANCE");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (RuntimeException e) {
            logger.error("Error marking attendance", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark attendance: " + e.getMessage());
            errorResponse.put("code", "SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Check if attendance exists for user and date
    @GetMapping("/check/{userId}/{date}")
    public ResponseEntity<?> checkAttendanceExists(
            @PathVariable long userId,
            @PathVariable String date,
            HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            LocalDate attendanceDate = LocalDate.parse(date);
            Map<String, Object> response = attendanceService.checkAttendanceExists(userId, attendanceDate);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Error checking attendance", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to check attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get attendance statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getAttendanceStats(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            Map<String, Object> stats = attendanceService.getAttendanceStats(start, end);
            return ResponseEntity.ok(stats);

        } catch (RuntimeException e) {
            logger.error("Failed to fetch attendance stats", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch attendance stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get user's attendance history
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAttendance(@PathVariable long userId, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            List<Map<String, Object>> records = attendanceService.getUserAttendanceHistory(userId);
            return ResponseEntity.ok(records);

        } catch (RuntimeException e) {
            logger.error("Failed to fetch user attendance", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch user attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Delete attendance record
    @DeleteMapping("/{attendanceId}")
    public ResponseEntity<?> deleteAttendance(@PathVariable long attendanceId, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            attendanceService.deleteAttendance(attendanceId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance record deleted successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Attendance not found: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (RuntimeException e) {
            logger.error("Failed to delete attendance", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to delete attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/my-attendance")
    public ResponseEntity<?> getMyAttendance(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated. Please log in."));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }

            List<Map<String, Object>> records = attendanceService.getMyAttendanceRecords(userId);
            return ResponseEntity.ok(records);

        } catch (RuntimeException e) {
            logger.error("Error fetching user attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch attendance data: " + e.getMessage()));
        }
    }

    // Get current user's attendance insights (for members)
    @GetMapping("/my-insights")
    public ResponseEntity<?> getMyAttendanceInsights(HttpSession session) {
        try {
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

            Map<String, Object> insights = attendanceService.getMyAttendanceInsights(userId);
            return ResponseEntity.ok(insights);

        } catch (RuntimeException e) {
            logger.error("Error fetching user insights", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch attendance insights: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get current user's subscription insights
    @GetMapping("/my-subscription-insights")
    public ResponseEntity<?> getMySubscriptionInsights(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated. Please log in."));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }

            Map<String, Object> insights = attendanceService.getMySubscriptionInsights(userId);
            return ResponseEntity.ok(insights);

        } catch (IllegalArgumentException e) {
            logger.warn("Membership error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message", e.getMessage(),
                            "hasExpiredMemberships", false));
        } catch (RuntimeException e) {
            logger.error("Error fetching subscription insights", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch subscription insights: " + e.getMessage()));
        }
    }

    // Get all memberships history with per-subscription statistics
    @GetMapping("/my-memberships-history")
    public ResponseEntity<?> getMyMembershipsHistory(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated. Please log in."));
            }

            Map<String, Object> response = attendanceService.getMyMembershipsHistory(userId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Error fetching memberships history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch memberships history: " + e.getMessage()));
        }
    }
}
