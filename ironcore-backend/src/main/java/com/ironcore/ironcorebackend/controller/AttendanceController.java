package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import com.ironcore.ironcorebackend.service.MembershipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipService membershipService;

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
            System.out.println("=== Getting Active Members ===");
            System.out.println("Date parameter: " + date);

            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            System.out.println("Target date: " + targetDate);

            // FIXED: Use getAllActiveMemberships instead of
            // getActiveMembershipsByUser(null)
            List<Membership> activeMemberships = membershipService.getAllActiveMemberships();
            System.out.println("Active memberships found: " + activeMemberships.size());

            // Filter for target date
            List<Membership> membershipsForDate = activeMemberships.stream()
                    .filter(membership -> {
                        if (membership.getMembershipActivatedDate() == null
                                || membership.getMembershipExpiryDate() == null) {
                            return false;
                        }

                        LocalDate activatedDate = membership.getMembershipActivatedDate().toLocalDate();
                        LocalDate expiryDate = membership.getMembershipExpiryDate().toLocalDate();

                        boolean isAfterOrOnStart = !targetDate.isBefore(activatedDate);
                        boolean isBeforeOrOnEnd = !targetDate.isAfter(expiryDate);

                        System.out.println("Membership " + membership.getId() + ": " +
                                "activated=" + activatedDate +
                                ", expiry=" + expiryDate +
                                ", targetDate=" + targetDate +
                                ", isAfterOrOnStart=" + isAfterOrOnStart +
                                ", isBeforeOrOnEnd=" + isBeforeOrOnEnd);

                        return isAfterOrOnStart && isBeforeOrOnEnd;
                    })
                    .collect(Collectors.toList());

            System.out.println("Memberships for date: " + membershipsForDate.size());

            // Group by user to get unique members
            Map<Long, Membership> uniqueMembers = new HashMap<>();
            for (Membership m : membershipsForDate) {
                if (m.getUser() != null) {
                    uniqueMembers.putIfAbsent(m.getUser().getId(), m);
                }
            }

            System.out.println("Unique members: " + uniqueMembers.size());

            // Get attendance records for this date
            List<Attendance> attendanceRecords = new ArrayList<>();
            try {
                attendanceRecords = attendanceRepository.findByAttendanceDate(targetDate);
                System.out.println("Attendance records found: " + attendanceRecords.size());
            } catch (Exception e) {
                System.out.println("No attendance records or error: " + e.getMessage());
                // Continue with empty list
            }

            Map<Long, Attendance> attendanceMap = new HashMap<>();
            for (Attendance a : attendanceRecords) {
                if (a.getUser() != null) {
                    attendanceMap.put(a.getUser().getId(), a);
                }
            }

            // Build response
            List<Map<String, Object>> members = new ArrayList<>();
            for (Membership membership : uniqueMembers.values()) {
                Map<String, Object> memberData = new HashMap<>();
                User user = membership.getUser();

                if (user == null)
                    continue;

                memberData.put("userId", user.getId());
                memberData.put("username", user.getUsername());
                memberData.put("email", user.getEmail());
                memberData.put("membershipType", membership.getMembershipType());
                memberData.put("membershipActivatedDate", membership.getMembershipActivatedDate());
                memberData.put("membershipExpiryDate", membership.getMembershipExpiryDate());

                // Check if user has attendance record for this date
                Attendance attendance = attendanceMap.get(user.getId());
                if (attendance != null) {
                    memberData.put("attendanceId", attendance.getId());
                    memberData.put("checkedIn", attendance.getCheckedIn());
                    memberData.put("checkInTime", attendance.getCheckInTime());
                    memberData.put("notes", attendance.getNotes());
                } else {
                    memberData.put("attendanceId", null);
                    memberData.put("checkedIn", false);
                    memberData.put("checkInTime", null);
                    memberData.put("notes", null);
                }

                members.add(memberData);
            }

            // Sort by username
            members.sort(Comparator.comparing(m -> (String) m.get("username")));

            System.out.println("Returning " + members.size() + " members");

            return ResponseEntity.ok(members);

        } catch (Exception e) {
            System.err.println("Error in getActiveMembers:");
            e.printStackTrace();
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
            System.out.println("=== Marking Attendance ===");
            System.out.println("Request: " + request);

            Long userId = Long.valueOf(request.get("userId").toString());
            String dateStr = request.get("date").toString();
            Boolean checkedIn = Boolean.valueOf(request.get("checkedIn").toString());
            String notes = null;
            if (request.containsKey("notes") && request.get("notes") != null) {
                notes = request.get("notes").toString();
            }

            LocalDate attendanceDate = LocalDate.parse(dateStr);
            Long adminId = (Long) session.getAttribute("userId");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // Get user's active membership
            List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);
            Membership activeMembership = activeMemberships.stream()
                    .filter(membership -> {
                        if (membership.getMembershipActivatedDate() == null ||
                                membership.getMembershipExpiryDate() == null) {
                            return false;
                        }

                        LocalDate activatedDate = membership.getMembershipActivatedDate().toLocalDate();
                        LocalDate expiryDate = membership.getMembershipExpiryDate().toLocalDate();

                        return !attendanceDate.isBefore(activatedDate) &&
                                !attendanceDate.isAfter(expiryDate);
                    })
                    .findFirst()
                    .orElse(null);

            if (activeMembership == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "User does not have an active membership for the selected date");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // FIXED: Check for existing attendance record
            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByUserIdAndAttendanceDate(userId, attendanceDate);

            Attendance attendance;
            boolean isUpdate = false;

            if (existingAttendance.isPresent()) {
                // FIXED: Record already exists - this is an UPDATE operation
                attendance = existingAttendance.get();
                isUpdate = true;

                // Update the record
                attendance.setCheckedIn(checkedIn);
                attendance.setCheckInTime(checkedIn ? LocalDateTime.now() : null);
                attendance.setCheckedByAdmin(admin);
                if (notes != null) {
                    attendance.setNotes(notes);
                }

                System.out.println("Updating existing attendance record: " + attendance.getId());
            } else {
                // FIXED: No existing record - this is a CREATE operation
                attendance = new Attendance();
                attendance.setUser(user);
                attendance.setAttendanceDate(attendanceDate);
                attendance.setCheckedIn(checkedIn);
                attendance.setCheckInTime(checkedIn ? LocalDateTime.now() : null);
                attendance.setCheckedByAdmin(admin);
                attendance.setMembershipType(activeMembership.getMembershipType());
                if (notes != null) {
                    attendance.setNotes(notes);
                }

                System.out.println("Creating new attendance record");
            }

            // FIXED: Use try-catch to handle database constraint violations
            try {
                Attendance saved = attendanceRepository.save(attendance);
                System.out.println("Attendance saved: " + saved.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message",
                        isUpdate ? "Attendance record updated successfully" : "Attendance record created successfully");
                response.put("attendanceId", saved.getId());
                response.put("checkedIn", saved.getCheckedIn());
                response.put("checkInTime", saved.getCheckInTime());
                response.put("isUpdate", isUpdate);

                return ResponseEntity.ok(response);

            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // FIXED: Handle race condition where duplicate was created between check and
                // save
                System.err.println("Duplicate attendance record detected: " + e.getMessage());

                // Try to fetch the existing record and return conflict error
                Optional<Attendance> conflictRecord = attendanceRepository
                        .findByUserIdAndAttendanceDate(userId, attendanceDate);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Attendance record already exists for this user on the selected date");
                errorResponse.put("code", "DUPLICATE_ATTENDANCE");
                if (conflictRecord.isPresent()) {
                    errorResponse.put("existingAttendanceId", conflictRecord.get().getId());
                    errorResponse.put("existingCheckedIn", conflictRecord.get().getCheckedIn());
                }
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

        } catch (Exception e) {
            System.err.println("Error marking attendance:");
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark attendance: " + e.getMessage());
            errorResponse.put("code", "SERVER_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Check if attendance exists for user and date
    // FIXED: Simplified check endpoint - returns existing record if found
    @GetMapping("/check/{userId}/{date}")
    public ResponseEntity<?> checkAttendanceExists(
            @PathVariable Long userId,
            @PathVariable String date,
            HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            LocalDate attendanceDate = LocalDate.parse(date);

            Optional<Attendance> existingAttendance = attendanceRepository
                    .findByUserIdAndAttendanceDate(userId, attendanceDate);

            Map<String, Object> response = new HashMap<>();

            if (existingAttendance.isPresent()) {
                Attendance attendance = existingAttendance.get();
                response.put("exists", true);
                response.put("attendanceId", attendance.getId());
                response.put("checkedIn", attendance.getCheckedIn());
                response.put("checkInTime", attendance.getCheckInTime());
                response.put("notes", attendance.getNotes());
                return ResponseEntity.ok(response);
            } else {
                response.put("exists", false);
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            System.err.println("Error checking attendance:");
            e.printStackTrace();
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

            List<Attendance> attendanceRecords = attendanceRepository.findByDateRange(start, end);

            long totalCheckIns = attendanceRecords.stream()
                    .filter(Attendance::getCheckedIn)
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCheckIns", totalCheckIns);
            stats.put("dateRange", Map.of("start", start, "end", end));
            stats.put("records", attendanceRecords.size());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch attendance stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get user's attendance history
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAttendance(@PathVariable Long userId, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            List<Attendance> attendanceRecords = attendanceRepository.findByUserIdOrderByAttendanceDateDesc(userId);

            List<Map<String, Object>> records = attendanceRecords.stream()
                    .map(a -> {
                        Map<String, Object> record = new HashMap<>();
                        record.put("id", a.getId());
                        record.put("date", a.getAttendanceDate());
                        record.put("checkedIn", a.getCheckedIn());
                        record.put("checkInTime", a.getCheckInTime());
                        record.put("membershipType", a.getMembershipType());
                        record.put("notes", a.getNotes());
                        if (a.getCheckedByAdmin() != null) {
                            record.put("checkedByAdmin", a.getCheckedByAdmin().getUsername());
                        }
                        return record;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(records);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch user attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Delete attendance record
    @DeleteMapping("/{attendanceId}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long attendanceId, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null)
            return accessCheck;

        try {
            Attendance attendance = attendanceRepository.findById(attendanceId)
                    .orElseThrow(() -> new RuntimeException("Attendance record not found"));

            attendanceRepository.delete(attendance);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance record deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
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

            System.out.println("=== Getting attendance for user: " + userId + " ===");

            // FIXED: Get current active membership
            List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);

            List<Map<String, Object>> records;

            if (activeMemberships.isEmpty()) {
                // No active membership - return empty or show all historical
                System.out.println("No active membership - returning empty attendance");
                records = new ArrayList<>();
            } else {
                // Get the most recent active membership
                Membership activeMembership = activeMemberships.stream()
                        .max(Comparator.comparing(Membership::getMembershipActivatedDate))
                        .orElse(activeMemberships.get(0));

                LocalDate subscriptionStart = activeMembership.getMembershipActivatedDate().toLocalDate();
                LocalDate subscriptionEnd = activeMembership.getMembershipExpiryDate().toLocalDate();

                // CRITICAL FIX: Get attendance ONLY for current subscription
                List<Attendance> attendanceRecords = attendanceRepository
                        .findByUserIdOrderByAttendanceDateDesc(userId)
                        .stream()
                        .filter(a -> {
                            LocalDate attDate = a.getAttendanceDate();
                            // ONLY include attendance within current subscription period
                            return !attDate.isBefore(subscriptionStart) && !attDate.isAfter(subscriptionEnd);
                        })
                        .collect(Collectors.toList());

                System.out.println("Found " + attendanceRecords.size() +
                        " attendance records for current subscription");

                records = attendanceRecords.stream()
                        .map(a -> {
                            Map<String, Object> record = new HashMap<>();
                            record.put("id", a.getId());
                            record.put("date", a.getAttendanceDate());
                            record.put("checkedIn", a.getCheckedIn());
                            record.put("checkInTime", a.getCheckInTime());
                            record.put("membershipType", a.getMembershipType());
                            record.put("notes", a.getNotes());
                            if (a.getCheckedByAdmin() != null) {
                                record.put("checkedByAdmin", a.getCheckedByAdmin().getUsername());
                            }
                            return record;
                        })
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(records);

        } catch (Exception e) {
            System.err.println("Error fetching user attendance:");
            e.printStackTrace();
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

            System.out.println("=== Getting insights for user: " + userId + " ===");

            // Get last 30 days of attendance
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);

            List<Attendance> attendanceRecords = attendanceRepository.findByDateRange(startDate, endDate)
                    .stream()
                    .filter(a -> a.getUser().getId().equals(userId))
                    .collect(Collectors.toList());

            // Calculate insights
            long totalSessions = attendanceRecords.size();
            long attendedSessions = attendanceRecords.stream()
                    .filter(Attendance::getCheckedIn)
                    .count();
            long missedSessions = totalSessions - attendedSessions;
            double attendanceRate = totalSessions > 0 ? (attendedSessions * 100.0) / totalSessions : 0;

            // Calculate current streak
            int currentStreak = calculateCurrentStreak(attendanceRecords);
            int maxStreak = calculateMaxStreak(attendanceRecords);

            Map<String, Object> insights = new HashMap<>();
            insights.put("totalSessions", totalSessions);
            insights.put("attendedSessions", attendedSessions);
            insights.put("missedSessions", missedSessions);
            insights.put("attendanceRate", Math.round(attendanceRate));
            insights.put("currentStreak", currentStreak);
            insights.put("maxStreak", maxStreak);
            insights.put("dateRange", Map.of("start", startDate, "end", endDate));

            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            System.err.println("Error fetching user insights:");
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch attendance insights: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Helper method to calculate current streak
    private int calculateCurrentStreak(List<Attendance> records) {
        if (records.isEmpty())
            return 0;

        // Sort by date descending (most recent first)
        List<Attendance> sorted = records.stream()
                .sorted((a1, a2) -> a2.getAttendanceDate().compareTo(a1.getAttendanceDate()))
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate currentDate = LocalDate.now();

        for (Attendance record : sorted) {
            if (record.getCheckedIn() && !record.getAttendanceDate().isBefore(currentDate.minusDays(30))) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    // Helper method to calculate max streak
    private int calculateMaxStreak(List<Attendance> records) {
        if (records.isEmpty())
            return 0;

        // Sort by date ascending
        List<Attendance> sorted = records.stream()
                .sorted(Comparator.comparing(Attendance::getAttendanceDate))
                .collect(Collectors.toList());

        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate previousDate = null;

        for (Attendance record : sorted) {
            if (record.getCheckedIn()) {
                if (previousDate == null || record.getAttendanceDate().equals(previousDate.plusDays(1))) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                maxStreak = Math.max(maxStreak, currentStreak);
                previousDate = record.getAttendanceDate();
            } else {
                currentStreak = 0;
                previousDate = null;
            }
        }

        return maxStreak;
    }

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

            System.out.println("=== Getting subscription insights for user: " + userId + " ===");

            // FIXED: Get ONLY the currently active membership
            List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);
            if (activeMemberships.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "message", "No active membership found",
                                "hasExpiredMemberships", hasExpiredMemberships(userId)));
            }

            // Get the most recent active membership
            Membership activeMembership = activeMemberships.stream()
                    .max(Comparator.comparing(Membership::getMembershipActivatedDate))
                    .orElse(activeMemberships.get(0));

            LocalDate subscriptionStart = activeMembership.getMembershipActivatedDate().toLocalDate();
            LocalDate subscriptionEnd = activeMembership.getMembershipExpiryDate().toLocalDate();
            LocalDate today = LocalDate.now();

            System.out.println("Active membership: ID=" + activeMembership.getId() +
                    ", Type=" + activeMembership.getMembershipType() +
                    ", Start=" + subscriptionStart +
                    ", End=" + subscriptionEnd);

            // CRITICAL FIX: Get attendance ONLY for the current subscription period
            List<Attendance> currentSubscriptionAttendance = attendanceRepository
                    .findByDateRange(subscriptionStart, today)
                    .stream()
                    .filter(a -> a.getUser().getId().equals(userId))
                    .filter(a -> {
                        // ONLY include attendance within current subscription dates
                        LocalDate attDate = a.getAttendanceDate();
                        return !attDate.isBefore(subscriptionStart) && !attDate.isAfter(subscriptionEnd);
                    })
                    .collect(Collectors.toList());

            System.out.println("Attendance records for CURRENT subscription: " +
                    currentSubscriptionAttendance.size());

            // Calculate subscription-based metrics
            Map<String, Object> insights = calculateSubscriptionBasedInsights(
                    activeMembership,
                    currentSubscriptionAttendance,
                    subscriptionStart,
                    subscriptionEnd,
                    today);

            // Add membership ID to track which subscription this is for
            insights.put("membershipId", activeMembership.getId());

            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            System.err.println("Error fetching subscription insights:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch subscription insights: " + e.getMessage()));
        }
    }


    private Map<String, Object> calculateSubscriptionBasedInsights(
            Membership membership,
            List<Attendance> attendanceRecords, // Already filtered for current subscription
            LocalDate subscriptionStart,
            LocalDate subscriptionEnd,
            LocalDate today) {

        Map<String, Object> insights = new HashMap<>();

        // Subscription period calculations
        long totalSubscriptionDays = java.time.temporal.ChronoUnit.DAYS.between(
                subscriptionStart, subscriptionEnd) + 1;
        long daysUsed = java.time.temporal.ChronoUnit.DAYS.between(
                subscriptionStart, today) + 1;
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                today, subscriptionEnd);

        // Ensure daysUsed doesn't exceed totalSubscriptionDays
        if (daysUsed > totalSubscriptionDays) {
            daysUsed = totalSubscriptionDays;
            daysRemaining = 0;
        }

        // Ensure daysRemaining is not negative
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }

        // Count only attended days (where checkedIn = true)
        long attendedDays = attendanceRecords.stream()
                .filter(Attendance::getCheckedIn)
                .count();

        System.out.println("Subscription calculations for current membership:");
        System.out.println("  Membership ID: " + membership.getId());
        System.out.println("  Period: " + subscriptionStart + " to " + subscriptionEnd);
        System.out.println("  Total subscription days: " + totalSubscriptionDays);
        System.out.println("  Days used so far: " + daysUsed);
        System.out.println("  Days remaining: " + daysRemaining);
        System.out.println("  Days attended: " + attendedDays);
        System.out.println("  Total attendance records: " + attendanceRecords.size());

        // Attendance Rate = (Days Attended / Days Used) * 100
        double attendanceRate = daysUsed > 0 ? (attendedDays * 100.0) / daysUsed : 0;

        // Subscription Utilization Rate = (Days Attended / Total Subscription Days) *
        // 100
        double subscriptionUtilizationRate = totalSubscriptionDays > 0 ? (attendedDays * 100.0) / totalSubscriptionDays
                : 0;

        // Days per week target
        double targetDaysPerWeek = getTargetDaysPerWeek(membership.getMembershipType());

        // Expected attendance by now
        double expectedAttendanceByNow = (daysUsed * targetDaysPerWeek) / 7.0;

        // Target Achievement
        double attendanceVsTarget = expectedAttendanceByNow > 0 ? (attendedDays * 100.0) / expectedAttendanceByNow
                : (attendedDays > 0 ? 100 : 0);

        // Remaining days target
        double remainingDaysTarget = (daysRemaining * targetDaysPerWeek) / 7.0;

        System.out.println("Calculated metrics:");
        System.out.println("  Attendance Rate: " + attendanceRate + "%");
        System.out.println("  Subscription Utilization: " + subscriptionUtilizationRate + "%");
        System.out.println("  Expected by now: " + expectedAttendanceByNow);
        System.out.println("  Target Achievement: " + attendanceVsTarget + "%");
        System.out.println("  Remaining target: " + remainingDaysTarget);

        // Generate feedback
        String feedback = generateSubscriptionBasedFeedback(
                attendanceRate,
                subscriptionUtilizationRate,
                attendanceVsTarget,
                daysRemaining,
                attendedDays,
                membership.getMembershipType());

        // Build response
        insights.put("subscriptionInfo", Map.of(
                "membershipType", membership.getMembershipType(),
                "startDate", subscriptionStart,
                "endDate", subscriptionEnd,
                "totalDays", totalSubscriptionDays,
                "daysUsed", daysUsed,
                "daysRemaining", daysRemaining,
                "targetDaysPerWeek", targetDaysPerWeek,
                "isExpiringSoon", daysRemaining <= 7 && daysRemaining > 0,
                "isExpired", daysRemaining == 0));

        insights.put("attendanceMetrics", Map.of(
                "attendedDays", attendedDays,
                "attendanceRate", Math.round(attendanceRate),
                "subscriptionUtilizationRate", Math.round(subscriptionUtilizationRate),
                "attendanceVsTarget", Math.round(attendanceVsTarget)));

        insights.put("targets", Map.of(
                "expectedAttendanceByNow", expectedAttendanceByNow,
                "remainingDaysTarget", remainingDaysTarget,
                "totalExpectedAttendance", (totalSubscriptionDays * targetDaysPerWeek) / 7.0));

        insights.put("feedback", feedback);
        insights.put("feedbackType", getFeedbackType(attendanceRate, attendanceVsTarget));

        return insights;
    }

    private double getTargetDaysPerWeek(String membershipType) {
        switch (membershipType.toUpperCase()) {
            case "BASIC":
                return 2.0;
            case "PREMIUM":
                return 3.0;
            case "ELITE":
                return 4.0;
            case "UNLIMITED":
                return 5.0;
            case "SILVER":
                return 2.0;
            case "GOLD":
                return 3.0;
            case "PLATINUM":
                return 4.0;
            case "MONTHLY":
                return 3.0;
            case "QUARTERLY":
                return 3.0;
            case "ANNUAL":
                return 4.0;
            default:
                return 3.0;
        }
    }

    // FIXED: Improved feedback generation
    private String generateSubscriptionBasedFeedback(
            double attendanceRate,
            double utilizationRate,
            double vsTarget,
            long daysRemaining,
            long attendedDays,
            String membershipType) {

        StringBuilder feedback = new StringBuilder();

        // Base feedback on attendance rate
        if (attendanceRate >= 80) {
            feedback.append("🎉 Excellent! You're attending ").append(Math.round(attendanceRate))
                    .append("% of your available days. ");
        } else if (attendanceRate >= 60) {
            feedback.append("👍 Good consistency! You're attending ").append(Math.round(attendanceRate))
                    .append("% of your available days. ");
        } else if (attendanceRate >= 40) {
            feedback.append("💪 Making progress! You're attending ").append(Math.round(attendanceRate))
                    .append("% of your available days. ");
        } else if (attendanceRate >= 20) {
            feedback.append("📊 You're attending ").append(Math.round(attendanceRate))
                    .append("% of your available days. ");
        } else {
            feedback.append("🎯 Let's boost your attendance! You're currently at ")
                    .append(Math.round(attendanceRate)).append("%. ");
        }

        // Target performance
        if (vsTarget >= 120) {
            feedback.append("You're crushing your target by ").append(Math.round(vsTarget - 100))
                    .append("%! Amazing! 🏆");
        } else if (vsTarget >= 100) {
            feedback.append("You're right on track with your goals! 🔥");
        } else if (vsTarget >= 80) {
            feedback.append("You're ").append(Math.round(100 - vsTarget))
                    .append("% behind target. Push harder! 💪");
        } else if (vsTarget >= 60) {
            feedback.append("Behind target but there's time to catch up! 🎯");
        } else {
            feedback.append("Let's increase your gym visits! 📈");
        }

        // Days remaining context
        if (daysRemaining == 0) {
            feedback.append(" ⚠️ Your subscription has expired! Renew to continue tracking.");
        } else if (daysRemaining <= 3) {
            feedback.append(" ⏰ Only ").append(daysRemaining).append(" days left - make them count!");
        } else if (daysRemaining <= 7) {
            feedback.append(" ⚡ ").append(daysRemaining).append(" days remaining - final push!");
        } else if (daysRemaining <= 14) {
            feedback.append(" You have ").append(daysRemaining).append(" days to maximize value!");
        }

        // Utilization advice
        if (utilizationRate < 20 && attendedDays < 5) {
            feedback.append(" Every session counts towards your goals!");
        } else if (utilizationRate > 50) {
            feedback.append(" Great value from your ").append(membershipType).append(" membership!");
        }

        return feedback.toString();
    }

   

    // FIXED: Better feedback type classification
    private String getFeedbackType(double attendanceRate, double vsTarget) {
        // Prioritize attendance rate, but consider target achievement
        if (attendanceRate >= 80 && vsTarget >= 100) {
            return "excellent";
        } else if (attendanceRate >= 60 && vsTarget >= 80) {
            return "good";
        } else if (attendanceRate >= 40 || vsTarget >= 60) {
            return "average";
        } else {
            return "needs-improvement";
        }
    }

    // NEW: Get all memberships history with per-subscription statistics
    @GetMapping("/my-memberships-history")
    public ResponseEntity<?> getMyMembershipsHistory(HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated. Please log in."));
            }

            // Get ALL memberships for this user (active and expired)
            List<Membership> allMemberships = membershipService.getAllMembershipsByUser(userId);

            // Sort by activation date (most recent first)
            allMemberships.sort((m1, m2) -> m2.getMembershipActivatedDate().compareTo(m1.getMembershipActivatedDate()));

            List<Map<String, Object>> membershipsWithStats = new ArrayList<>();

            for (Membership membership : allMemberships) {
                LocalDate start = membership.getMembershipActivatedDate().toLocalDate();
                LocalDate end = membership.getMembershipExpiryDate().toLocalDate();
                LocalDate today = LocalDate.now();

                // Get attendance for THIS specific membership period
                List<Attendance> membershipAttendance = attendanceRepository
                        .findByDateRange(start, end)
                        .stream()
                        .filter(a -> a.getUser().getId().equals(userId))
                        .filter(a -> {
                            LocalDate attDate = a.getAttendanceDate();
                            return !attDate.isBefore(start) && !attDate.isAfter(end);
                        })
                        .collect(Collectors.toList());

                long attendedDays = membershipAttendance.stream()
                        .filter(Attendance::getCheckedIn)
                        .count();

                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                boolean isActive = membership.isCurrentlyActive();
                boolean isExpired = membership.isExpired();

                Map<String, Object> membershipData = new HashMap<>();
                membershipData.put("membershipId", membership.getId());
                membershipData.put("membershipType", membership.getMembershipType());
                membershipData.put("startDate", start);
                membershipData.put("endDate", end);
                membershipData.put("totalDays", totalDays);
                membershipData.put("attendedDays", attendedDays);
                membershipData.put("isActive", isActive);
                membershipData.put("isExpired", isExpired);
                membershipData.put("utilizationRate",
                        totalDays > 0 ? Math.round((attendedDays * 100.0) / totalDays) : 0);

                membershipsWithStats.add(membershipData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("memberships", membershipsWithStats);
            response.put("totalMemberships", membershipsWithStats.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error fetching memberships history:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch memberships history: " + e.getMessage()));
        }
    }

    // Helper method to check if user has expired memberships
    private boolean hasExpiredMemberships(Long userId) {
        try {
            List<Membership> allMemberships = membershipService.getAllMembershipsByUser(userId);
            return allMemberships.stream().anyMatch(Membership::isExpired);
        } catch (Exception e) {
            return false;
        }
    }
}
