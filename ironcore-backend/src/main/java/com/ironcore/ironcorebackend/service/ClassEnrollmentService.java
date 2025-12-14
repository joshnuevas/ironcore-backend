package com.ironcore.ironcorebackend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;

@Service
@Transactional
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    public ClassEnrollmentService(ClassEnrollmentRepository classEnrollmentRepository,
                                 ScheduleRepository scheduleRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    // ================================
    // Basic CRUD used by controller
    // ================================
    public ClassEnrollment saveEnrollment(ClassEnrollment enrollment) {
        if (enrollment == null) throw new IllegalArgumentException("Enrollment cannot be null");
        return classEnrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollment> getAllEnrollments() {
        return classEnrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ClassEnrollment> getEnrollmentById(Long enrollmentId) {
        if (enrollmentId == null) throw new IllegalArgumentException("enrollmentId cannot be null");
        return classEnrollmentRepository.findById(enrollmentId);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollment> getEnrollmentsByUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        return classEnrollmentRepository.findByUserId(userId);
    }

    public void completeSession(Long enrollmentId) {
        if (enrollmentId == null) throw new IllegalArgumentException("enrollmentId cannot be null");
        classEnrollmentRepository.markSessionCompleted(enrollmentId);
    }

    // ================================
    // ✅ Schedule conflict check (OVERLAP)
    // ================================
    @Transactional(readOnly = true)
    public ScheduleConflictResponse checkScheduleConflict(Long userId, Long scheduleId) {
        if (userId == null || scheduleId == null) {
            return ScheduleConflictResponse.noConflict();
        }

        Schedule target = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

        LocalDate targetDate = target.getDate();
        String targetSlot = target.getTimeSlot();

        if (targetDate == null || targetSlot == null) {
            return ScheduleConflictResponse.noConflict();
        }

        TimeRange targetRange = parseTimeSlot(targetSlot);

        // Get ALL active enrollments on same date, then do overlap check
        List<ClassEnrollment> sameDay = classEnrollmentRepository.findActiveEnrollmentsOnDate(userId, targetDate);

        for (ClassEnrollment ce : sameDay) {
            if (ce == null || ce.getSchedule() == null || ce.getSchedule().getTimeSlot() == null) continue;

            Schedule existingSchedule = ce.getSchedule();

            // Optional: ignore if it's literally the same schedule record
            if (existingSchedule.getId() != null && existingSchedule.getId().equals(scheduleId)) {
                continue;
            }

            TimeRange existingRange = parseTimeSlot(existingSchedule.getTimeSlot());

            if (overlaps(targetRange, existingRange)) {
                // Return details your React modal expects
                return ScheduleConflictResponse.conflict(
                        existingSchedule.getClassEntity() != null ? existingSchedule.getClassEntity().getName() : "Class",
                        existingSchedule.getDate(),
                        existingSchedule.getDay(),
                        existingSchedule.getTimeSlot(),
                        ce.getTransaction() != null ? ce.getTransaction().getTransactionCode() : null
                );
            }
        }

        return ScheduleConflictResponse.noConflict();
    }

    // ================================
    // Response object for controller/UI
    // ================================
    public static class ScheduleConflictResponse {
        private boolean hasConflict;
        private String className;
        private LocalDate scheduleDate;
        private String scheduleDay;
        private String scheduleTime;
        private String transactionCode;

        public static ScheduleConflictResponse noConflict() {
            ScheduleConflictResponse r = new ScheduleConflictResponse();
            r.hasConflict = false;
            return r;
        }

        public static ScheduleConflictResponse conflict(String className,
                                                       LocalDate scheduleDate,
                                                       String scheduleDay,
                                                       String scheduleTime,
                                                       String transactionCode) {
            ScheduleConflictResponse r = new ScheduleConflictResponse();
            r.hasConflict = true;
            r.className = className;
            r.scheduleDate = scheduleDate;
            r.scheduleDay = scheduleDay;
            r.scheduleTime = scheduleTime;
            r.transactionCode = transactionCode;
            return r;
        }

        public boolean isHasConflict() { return hasConflict; }
        public void setHasConflict(boolean hasConflict) { this.hasConflict = hasConflict; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }

        public String getScheduleDay() { return scheduleDay; }
        public void setScheduleDay(String scheduleDay) { this.scheduleDay = scheduleDay; }

        public String getScheduleTime() { return scheduleTime; }
        public void setScheduleTime(String scheduleTime) { this.scheduleTime = scheduleTime; }

        public String getTransactionCode() { return transactionCode; }
        public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    }

    // ================================
    // Overlap helpers
    // ================================
    private static class TimeRange {
        final LocalTime start;
        final LocalTime end;
        TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }

    private TimeRange parseTimeSlot(String timeSlot) {
        if (timeSlot == null) throw new IllegalArgumentException("timeSlot is null");

        // supports: "7:00-8:00", "7:00 - 8:00", "7:00 to 8:00", and dash variants
        String cleaned = timeSlot.trim()
                .toLowerCase(Locale.ROOT)
                .replace("–", "-")
                .replace("—", "-")
                .replace(" to ", "-")
                .replaceAll("\\s+", "");

        String[] parts = cleaned.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid timeSlot format: " + timeSlot);
        }

        LocalTime start = parseLocalTimeFlexible(parts[0]);
        LocalTime end = parseLocalTimeFlexible(parts[1]);

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time: " + timeSlot);
        }

        return new TimeRange(start, end);
    }

    private LocalTime parseLocalTimeFlexible(String s) {
        if (s == null) throw new IllegalArgumentException("time value is null");

        String raw = s.trim();

        // 1) Try 24-hour: 7:00 / 19:30 / 07:00
        try {
            return LocalTime.parse(raw, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException ignored) {}

        // Normalize AM/PM formats:
        // "7:00 AM" -> "7:00AM"
        // "7:00 pm" -> "7:00PM"
        String normalized = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");

        // 2) Try 12-hour: 7:00AM
        try {
            return LocalTime.parse(normalized, DateTimeFormatter.ofPattern("h:mma", Locale.ROOT));
        } catch (DateTimeParseException ignored) {}

        // 3) Try 12-hour with leading zero: 07:00AM
        return LocalTime.parse(normalized, DateTimeFormatter.ofPattern("hh:mma", Locale.ROOT));
    }

    private boolean overlaps(TimeRange a, TimeRange b) {
        // overlap if a.start < b.end && a.end > b.start
        return a.start.isBefore(b.end) && a.end.isAfter(b.start);
    }
}
