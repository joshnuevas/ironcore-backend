package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
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
        
        if (!Boolean.TRUE.equals(user.getIsAdmin())) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Access denied. Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
        
        return null;
    }

    // Get active members with valid memberships for today
    @GetMapping("/active-members")
    public ResponseEntity<?> getActiveMembers(@RequestParam(required = false) String date, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

        try {
            System.out.println("=== Getting Active Members ===");
            System.out.println("Date parameter: " + date);
            
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            System.out.println("Target date: " + targetDate);
            
            LocalDateTime targetDateTime = targetDate.atStartOfDay();
            System.out.println("Target datetime: " + targetDateTime);

            // Get all transactions
            List<Transaction> allTransactions = transactionRepository.findAll();
            System.out.println("Total transactions: " + allTransactions.size());
            
            // Filter for active memberships
            List<Transaction> activeMemberships = allTransactions.stream()
                .filter(t -> {
                    boolean hasMembershipType = t.getMembershipType() != null;
                    boolean hasActivatedDate = t.getMembershipActivatedDate() != null;
                    boolean hasExpiryDate = t.getMembershipExpiryDate() != null;
                    boolean isCompleted = t.getPaymentStatus() == PaymentStatus.COMPLETED;
                    
                    // Compare dates only (ignore time)
                    boolean isAfterStart = true;
                    boolean isBeforeEnd = true;
                    
                    if (hasActivatedDate) {
                        LocalDate activatedDate = t.getMembershipActivatedDate().toLocalDate();
                        isAfterStart = !targetDate.isBefore(activatedDate);
                    }
                    
                    if (hasExpiryDate) {
                        LocalDate expiryDate = t.getMembershipExpiryDate().toLocalDate();
                        isBeforeEnd = !targetDate.isAfter(expiryDate);
                    }
                    
                    boolean result = hasMembershipType && hasActivatedDate && hasExpiryDate 
                           && isCompleted && isAfterStart && isBeforeEnd;
                    
                    if (hasMembershipType) {
                        System.out.println("Transaction " + t.getId() + ": " + 
                            "type=" + hasMembershipType + 
                            ", activated=" + hasActivatedDate + 
                            ", expiry=" + hasExpiryDate + 
                            ", completed=" + isCompleted + 
                            ", afterStart=" + isAfterStart + 
                            ", beforeEnd=" + isBeforeEnd + 
                            ", result=" + result);
                    }
                    
                    return result;
                })
                .collect(Collectors.toList());
            
            System.out.println("Active memberships found: " + activeMemberships.size());

            // Group by user to get unique members
            Map<Long, Transaction> uniqueMembers = new HashMap<>();
            for (Transaction t : activeMemberships) {
                if (t.getUser() != null) {
                    uniqueMembers.putIfAbsent(t.getUser().getId(), t);
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
            for (Transaction membership : uniqueMembers.values()) {
                Map<String, Object> memberData = new HashMap<>();
                User user = membership.getUser();
                
                if (user == null) continue;
                
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

    // Mark attendance (check-in or check-out)
    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request, HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

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
            LocalDateTime targetDateTime = attendanceDate.atStartOfDay();
            Transaction activeMembership = transactionRepository.findAll().stream()
                .filter(t -> t.getUser() != null 
                          && t.getUser().getId().equals(userId)
                          && t.getMembershipType() != null 
                          && t.getMembershipActivatedDate() != null
                          && t.getMembershipExpiryDate() != null
                          && t.getPaymentStatus() == PaymentStatus.COMPLETED
                          && !targetDateTime.isBefore(t.getMembershipActivatedDate())
                          && !targetDateTime.isAfter(t.getMembershipExpiryDate()))
                .findFirst()
                .orElse(null);

            if (activeMembership == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "User does not have an active membership");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Check if attendance record exists
            Optional<Attendance> existingAttendance = attendanceRepository
                .findByUserIdAndAttendanceDate(userId, attendanceDate);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
                attendance.setCheckedIn(checkedIn);
                attendance.setCheckInTime(checkedIn ? LocalDateTime.now() : null);
                attendance.setCheckedByAdmin(admin);
                if (notes != null) {
                    attendance.setNotes(notes);
                }
            } else {
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
            }

            Attendance saved = attendanceRepository.save(attendance);
            System.out.println("Attendance saved: " + saved.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", checkedIn ? "Member checked in successfully" : "Member check-in removed");
            response.put("attendanceId", saved.getId());
            response.put("checkedIn", saved.getCheckedIn());
            response.put("checkInTime", saved.getCheckInTime());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error marking attendance:");
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to mark attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get attendance statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getAttendanceStats(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                HttpSession session) {
        ResponseEntity<?> accessCheck = verifyAdminAccess(session);
        if (accessCheck != null) return accessCheck;

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
        if (accessCheck != null) return accessCheck;

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
        if (accessCheck != null) return accessCheck;

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
}