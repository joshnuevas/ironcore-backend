package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    public ClassEnrollmentService(ClassEnrollmentRepository classEnrollmentRepository,
                                  ScheduleRepository scheduleRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public ClassEnrollment saveEnrollment(final ClassEnrollment enrollment) {
        return classEnrollmentRepository.save(Objects.requireNonNull(enrollment));
    }
    public List<ClassEnrollment> getAllEnrollments() {
        return classEnrollmentRepository.findAll();
    }

    public Optional<ClassEnrollment> getEnrollmentById(Long id) {
        // Make analyzer happy: id must be non-null here
        return classEnrollmentRepository.findById(Objects.requireNonNull(id));
    }

    // UPDATED: Use the correct repository method name
    public Optional<ClassEnrollment> getEnrollmentByTransactionCode(String code) {
        return classEnrollmentRepository.findByTransaction_TransactionCode(code);
    }

    public List<ClassEnrollment> getEnrollmentsByUser(Long userId) {
        return classEnrollmentRepository.findByUserId(Objects.requireNonNull(userId));
    }

    public void completeSession(Long enrollmentId) {
        classEnrollmentRepository.markSessionCompleted(Objects.requireNonNull(enrollmentId));
    }

    // Get enrollments by schedule
    public List<ClassEnrollment> getEnrollmentsBySchedule(Long scheduleId) {
        return classEnrollmentRepository.findByScheduleId(Objects.requireNonNull(scheduleId));
    }

    // Get paid enrollments
    public List<ClassEnrollment> getPaidEnrollments() {
        return classEnrollmentRepository.findPaidEnrollments();
    }

    // ============================
    // 🔍 Schedule conflict check
    // ============================
    @Transactional(readOnly = true)
    public ScheduleConflictResponse checkScheduleConflict(Long userId, Long scheduleId) {
        if (userId == null || scheduleId == null) {
            throw new IllegalArgumentException("User ID and Schedule ID cannot be null");
        }

        Schedule schedule = scheduleRepository.findById(Objects.requireNonNull(scheduleId))
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

        var conflicts = classEnrollmentRepository.findConflictingSchedules(
                Objects.requireNonNull(userId),
                schedule.getDate(),
                schedule.getTimeSlot()
        );

        ScheduleConflictResponse resp = new ScheduleConflictResponse();
        if (conflicts.isEmpty()) {
            resp.setHasConflict(false);
            return resp;
        }

        // Use first conflicting enrollment for display
        ClassEnrollment conflict = conflicts.get(0);

        resp.setHasConflict(true);
        resp.setClassName(conflict.getClassEntity().getName());
        resp.setScheduleDay(schedule.getDay());
        resp.setScheduleTime(schedule.getTimeSlot());
        resp.setScheduleDate(schedule.getDate().toString());
        resp.setTransactionCode(conflict.getTransactionCode());

        return resp;
    }

    // ==============================================
    // DTO returned to the frontend for conflict UI
    // ==============================================
    public static class ScheduleConflictResponse {
        private boolean hasConflict;
        private String className;
        private String scheduleDay;
        private String scheduleTime;
        private String scheduleDate;
        private String transactionCode;

        public boolean isHasConflict() {
            return hasConflict;
        }

        public void setHasConflict(boolean hasConflict) {
            this.hasConflict = hasConflict;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getScheduleDay() {
            return scheduleDay;
        }

        public void setScheduleDay(String scheduleDay) {
            this.scheduleDay = scheduleDay;
        }

        public String getScheduleTime() {
            return scheduleTime;
        }

        public void setScheduleTime(String scheduleTime) {
            this.scheduleTime = scheduleTime;
        }

        public String getScheduleDate() {
            return scheduleDate;
        }

        public void setScheduleDate(String scheduleDate) {
            this.scheduleDate = scheduleDate;
        }

        public String getTransactionCode() {
            return transactionCode;
        }

        public void setTransactionCode(String transactionCode) {
            this.transactionCode = transactionCode;
        }
    }
}
