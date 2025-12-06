package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.Attendance;
import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.AttendanceRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipService membershipService;

    public List<Map<String, Object>> getActiveMembersForDate(LocalDate targetDate) {
        List<Membership> activeMemberships = membershipService.getAllActiveMemberships();

        List<Membership> membershipsForDate = activeMemberships.stream()
                .filter(membership -> {
                    if (membership.getMembershipActivatedDate() == null
                            || membership.getMembershipExpiryDate() == null) {
                        return false;
                    }

                    LocalDate activatedDate = membership.getMembershipActivatedDate().toLocalDate();
                    LocalDate expiryDate = membership.getMembershipExpiryDate().toLocalDate();

                    return !targetDate.isBefore(activatedDate) && !targetDate.isAfter(expiryDate);
                })
                .collect(Collectors.toList());

        Map<Long, Membership> uniqueMembers = new HashMap<>();
        for (Membership m : membershipsForDate) {
            if (m.getUser() != null) {
                uniqueMembers.putIfAbsent(m.getUser().getId(), m);
            }
        }

        List<Attendance> attendanceRecords = new ArrayList<>();
        try {
            attendanceRecords = attendanceRepository.findByAttendanceDate(targetDate);
        } catch (RuntimeException e) {
            logger.debug("No attendance records for date: " + targetDate);
        }

        Map<Long, Attendance> attendanceMap = new HashMap<>();
        for (Attendance a : attendanceRecords) {
            if (a.getUser() != null) {
                attendanceMap.put(a.getUser().getId(), a);
            }
        }

        List<Map<String, Object>> members = new ArrayList<>();
        for (Membership membership : uniqueMembers.values()) {
            User user = membership.getUser();
            if (user == null) continue;

            Map<String, Object> memberData = new HashMap<>();
            memberData.put("userId", user.getId());
            memberData.put("username", user.getUsername());
            memberData.put("email", user.getEmail());
            memberData.put("membershipType", membership.getMembershipType());
            memberData.put("membershipActivatedDate", membership.getMembershipActivatedDate());
            memberData.put("membershipExpiryDate", membership.getMembershipExpiryDate());

            Attendance attendance = attendanceMap.get(user.getId());
            if (attendance != null) {
                memberData.put("attendanceId", attendance.getId());
                memberData.put("checkedIn", attendance.getCheckedIn());
                memberData.put("checkInTime", attendance.getCheckInTime());
            } else {
                memberData.put("attendanceId", null);
                memberData.put("checkedIn", false);
                memberData.put("checkInTime", null);
            }

            members.add(memberData);
        }

        members.sort(Comparator.comparing(m -> (String) m.get("username")));
        return members;
    }

    public Map<String, Object> markAttendance(long userId, LocalDate attendanceDate, Boolean checkedIn, long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);
        Membership activeMembership = activeMemberships.stream()
                .filter(membership -> {
                    if (membership.getMembershipActivatedDate() == null ||
                            membership.getMembershipExpiryDate() == null) {
                        return false;
                    }

                    LocalDate activatedDate = membership.getMembershipActivatedDate().toLocalDate();
                    LocalDate expiryDate = membership.getMembershipExpiryDate().toLocalDate();

                    return !attendanceDate.isBefore(activatedDate) && !attendanceDate.isAfter(expiryDate);
                })
                .findFirst()
                .orElse(null);

        if (activeMembership == null) {
            throw new IllegalArgumentException("User does not have an active membership for the selected date");
        }

        Optional<Attendance> existingAttendance = attendanceRepository
                .findByUserIdAndAttendanceDate(userId, attendanceDate);

        Attendance attendance;
        boolean isUpdate = false;

        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
            isUpdate = true;
            attendance.setCheckedIn(checkedIn);
            attendance.setCheckInTime(checkedIn ? LocalDateTime.now() : null);
            attendance.setCheckedByAdmin(admin);
        } else {
            attendance = new Attendance();
            attendance.setUser(user);
            attendance.setAttendanceDate(attendanceDate);
            attendance.setCheckedIn(checkedIn);
            attendance.setCheckInTime(checkedIn ? LocalDateTime.now() : null);
            attendance.setCheckedByAdmin(admin);
            attendance.setMembershipType(activeMembership.getMembershipType());
        }

        Attendance saved = attendanceRepository.save(attendance);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", isUpdate ? "Attendance record updated successfully" : "Attendance record created successfully");
        response.put("attendanceId", saved.getId());
        response.put("checkedIn", saved.getCheckedIn());
        response.put("checkInTime", saved.getCheckInTime());
        response.put("isUpdate", isUpdate);

        return response;
    }

    public Map<String, Object> checkAttendanceExists(long userId, LocalDate attendanceDate) {
        Optional<Attendance> existingAttendance = attendanceRepository
                .findByUserIdAndAttendanceDate(userId, attendanceDate);

        Map<String, Object> response = new HashMap<>();

        if (existingAttendance.isPresent()) {
            Attendance attendance = existingAttendance.get();
            response.put("exists", true);
            response.put("attendanceId", attendance.getId());
            response.put("checkedIn", attendance.getCheckedIn());
            response.put("checkInTime", attendance.getCheckInTime());
        } else {
            response.put("exists", false);
        }

        return response;
    }

    public Map<String, Object> getAttendanceStats(LocalDate start, LocalDate end) {
        List<Attendance> attendanceRecords = attendanceRepository.findByDateRange(start, end);

        long totalCheckIns = attendanceRecords.stream()
                .filter(Attendance::getCheckedIn)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCheckIns", totalCheckIns);
        stats.put("dateRange", Map.of("start", start, "end", end));
        stats.put("records", attendanceRecords.size());

        return stats;
    }

    public List<Map<String, Object>> getUserAttendanceHistory(long userId) {
        List<Attendance> attendanceRecords = attendanceRepository.findByUserIdOrderByAttendanceDateDesc(userId);

        return attendanceRecords.stream()
                .map(a -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", a.getId());
                    record.put("date", a.getAttendanceDate());
                    record.put("checkedIn", a.getCheckedIn());
                    record.put("checkInTime", a.getCheckInTime());
                    record.put("membershipType", a.getMembershipType());
                    if (a.getCheckedByAdmin() != null) {
                        record.put("checkedByAdmin", a.getCheckedByAdmin().getUsername());
                    }
                    return record;
                })
                .collect(Collectors.toList());
    }

    public void deleteAttendance(long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new IllegalArgumentException("Attendance record not found");
        }
        attendanceRepository.deleteById(attendanceId);
    }

    public List<Map<String, Object>> getMyAttendanceRecords(long userId) {
        List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);

        if (activeMemberships.isEmpty()) {
            return new ArrayList<>();
        }

        Membership activeMembership = activeMemberships.stream()
                .max(Comparator.comparing(Membership::getMembershipActivatedDate))
                .orElse(activeMemberships.get(0));

        LocalDate subscriptionStart = activeMembership.getMembershipActivatedDate().toLocalDate();
        LocalDate subscriptionEnd = activeMembership.getMembershipExpiryDate().toLocalDate();

        List<Attendance> attendanceRecords = attendanceRepository
                .findByUserIdOrderByAttendanceDateDesc(userId)
                .stream()
                .filter(a -> {
                    LocalDate attDate = a.getAttendanceDate();
                    return !attDate.isBefore(subscriptionStart) && !attDate.isAfter(subscriptionEnd);
                })
                .collect(Collectors.toList());

        return attendanceRecords.stream()
                .map(a -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", a.getId());
                    record.put("date", a.getAttendanceDate());
                    record.put("checkedIn", a.getCheckedIn());
                    record.put("checkInTime", a.getCheckInTime());
                    record.put("membershipType", a.getMembershipType());
                    if (a.getCheckedByAdmin() != null) {
                        record.put("checkedByAdmin", a.getCheckedByAdmin().getUsername());
                    }
                    return record;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMyAttendanceInsights(long userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<Attendance> attendanceRecords = attendanceRepository.findByDateRange(startDate, endDate)
                .stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        long totalSessions = attendanceRecords.size();
        long attendedSessions = attendanceRecords.stream()
                .filter(Attendance::getCheckedIn)
                .count();
        long missedSessions = totalSessions - attendedSessions;
        double attendanceRate = totalSessions > 0 ? (attendedSessions * 100.0) / totalSessions : 0;

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

        return insights;
    }

    public Map<String, Object> getMySubscriptionInsights(long userId) {
        List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);
        if (activeMemberships.isEmpty()) {
            throw new IllegalArgumentException("No active membership found");
        }

        Membership activeMembership = activeMemberships.stream()
                .max(Comparator.comparing(Membership::getMembershipActivatedDate))
                .orElse(activeMemberships.get(0));

        LocalDate subscriptionStart = activeMembership.getMembershipActivatedDate().toLocalDate();
        LocalDate subscriptionEnd = activeMembership.getMembershipExpiryDate().toLocalDate();
        LocalDate today = LocalDate.now();

        List<Attendance> currentSubscriptionAttendance = attendanceRepository
                .findByDateRange(subscriptionStart, today)
                .stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .filter(a -> {
                    LocalDate attDate = a.getAttendanceDate();
                    return !attDate.isBefore(subscriptionStart) && !attDate.isAfter(subscriptionEnd);
                })
                .collect(Collectors.toList());

        Map<String, Object> insights = calculateSubscriptionBasedInsights(
                activeMembership,
                currentSubscriptionAttendance,
                subscriptionStart,
                subscriptionEnd,
                today);

        insights.put("membershipId", activeMembership.getId());
        return insights;
    }

    public Map<String, Object> getMyMembershipsHistory(long userId) {
        List<Membership> allMemberships = membershipService.getAllMembershipsByUser(userId);
        allMemberships.sort((m1, m2) -> m2.getMembershipActivatedDate().compareTo(m1.getMembershipActivatedDate()));

        List<Map<String, Object>> membershipsWithStats = new ArrayList<>();

        for (Membership membership : allMemberships) {
            LocalDate start = membership.getMembershipActivatedDate().toLocalDate();
            LocalDate end = membership.getMembershipExpiryDate().toLocalDate();

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
            membershipData.put("utilizationRate", totalDays > 0 ? Math.round((attendedDays * 100.0) / totalDays) : 0);

            membershipsWithStats.add(membershipData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("memberships", membershipsWithStats);
        response.put("totalMemberships", membershipsWithStats.size());

        return response;
    }

    public boolean hasExpiredMemberships(Long userId) {
        try {
            List<Membership> allMemberships = membershipService.getAllMembershipsByUser(userId);
            return allMemberships.stream().anyMatch(Membership::isExpired);
        } catch (RuntimeException e) {
            logger.error("Error checking expired memberships", e);
            return false;
        }
    }

    // ...existing helper methods...
    private int calculateCurrentStreak(List<Attendance> records) {
        if (records.isEmpty())
            return 0;

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

    private int calculateMaxStreak(List<Attendance> records) {
        if (records.isEmpty())
            return 0;

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

    private Map<String, Object> calculateSubscriptionBasedInsights(
            Membership membership,
            List<Attendance> attendanceRecords,
            LocalDate subscriptionStart,
            LocalDate subscriptionEnd,
            LocalDate today) {

        Map<String, Object> insights = new HashMap<>();

        long totalSubscriptionDays = java.time.temporal.ChronoUnit.DAYS.between(subscriptionStart, subscriptionEnd) + 1;
        long daysUsed = java.time.temporal.ChronoUnit.DAYS.between(subscriptionStart, today) + 1;
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, subscriptionEnd);

        if (daysUsed > totalSubscriptionDays) {
            daysUsed = totalSubscriptionDays;
            daysRemaining = 0;
        }

        if (daysRemaining < 0) {
            daysRemaining = 0;
        }

        long attendedDays = attendanceRecords.stream()
                .filter(Attendance::getCheckedIn)
                .count();

        double attendanceRate = daysUsed > 0 ? (attendedDays * 100.0) / daysUsed : 0;
        double subscriptionUtilizationRate = totalSubscriptionDays > 0 ? (attendedDays * 100.0) / totalSubscriptionDays : 0;
        double targetDaysPerWeek = getTargetDaysPerWeek(membership.getMembershipType());
        double expectedAttendanceByNow = (daysUsed * targetDaysPerWeek) / 7.0;
        double attendanceVsTarget = expectedAttendanceByNow > 0 ? (attendedDays * 100.0) / expectedAttendanceByNow : (attendedDays > 0 ? 100 : 0);
        double remainingDaysTarget = (daysRemaining * targetDaysPerWeek) / 7.0;

        String feedback = generateSubscriptionBasedFeedback(attendanceRate, subscriptionUtilizationRate, attendanceVsTarget, daysRemaining, attendedDays, membership.getMembershipType());

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
        if (membershipType == null) {
            return 3.0;
        }
        return switch (membershipType.toUpperCase()) {
            case "BASIC", "SILVER" -> 2.0;
            case "PREMIUM", "GOLD", "MONTHLY", "QUARTERLY" -> 3.0;
            case "ELITE", "PLATINUM", "ANNUAL" -> 4.0;
            case "UNLIMITED" -> 5.0;
            default -> 3.0;
        };
    }

    private String generateSubscriptionBasedFeedback(double attendanceRate, double utilizationRate, double vsTarget, long daysRemaining, long attendedDays, String membershipType) {
        StringBuilder feedback = new StringBuilder();

        if (attendanceRate >= 80) {
            feedback.append("🎉 Excellent! You're attending ").append(Math.round(attendanceRate)).append("% of your available days. ");
        } else if (attendanceRate >= 60) {
            feedback.append("👍 Good consistency! You're attending ").append(Math.round(attendanceRate)).append("% of your available days. ");
        } else if (attendanceRate >= 40) {
            feedback.append("💪 Making progress! You're attending ").append(Math.round(attendanceRate)).append("% of your available days. ");
        } else if (attendanceRate >= 20) {
            feedback.append("📊 You're attending ").append(Math.round(attendanceRate)).append("% of your available days. ");
        } else {
            feedback.append("🎯 Let's boost your attendance! You're currently at ").append(Math.round(attendanceRate)).append("%. ");
        }

        if (vsTarget >= 120) {
            feedback.append("You're crushing your target by ").append(Math.round(vsTarget - 100)).append("%! Amazing! 🏆");
        } else if (vsTarget >= 100) {
            feedback.append("You're right on track with your goals! 🔥");
        } else if (vsTarget >= 80) {
            feedback.append("You're ").append(Math.round(100 - vsTarget)).append("% behind target. Push harder! 💪");
        } else if (vsTarget >= 60) {
            feedback.append("Behind target but there's time to catch up! 🎯");
        } else {
            feedback.append("Let's increase your gym visits! 📈");
        }

        if (daysRemaining == 0) {
            feedback.append(" ⚠️ Your subscription has expired! Renew to continue tracking.");
        } else if (daysRemaining <= 3) {
            feedback.append(" ⏰ Only ").append(daysRemaining).append(" days left - make them count!");
        } else if (daysRemaining <= 7) {
            feedback.append(" ⚡ ").append(daysRemaining).append(" days remaining - final push!");
        } else if (daysRemaining <= 14) {
            feedback.append(" You have ").append(daysRemaining).append(" days to maximize value!");
        }

        if (utilizationRate < 20 && attendedDays < 5) {
            feedback.append(" Every session counts towards your goals!");
        } else if (utilizationRate > 50) {
            feedback.append(" Great value from your ").append(membershipType).append(" membership!");
        }

        return feedback.toString();
    }

    private String getFeedbackType(double attendanceRate, double vsTarget) {
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
}
