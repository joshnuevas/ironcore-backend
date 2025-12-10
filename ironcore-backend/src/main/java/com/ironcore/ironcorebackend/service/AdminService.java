package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.TransactionRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ClassEnrollmentService classEnrollmentService;

    

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        long activeSchedules = scheduleRepository.count();

        long totalMembers = userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsAdmin()))
                .count();

        int availableSlots = scheduleRepository.findAll().stream()
                .mapToInt(schedule -> schedule.getMaxParticipants() - schedule.getEnrolledCount())
                .sum();

        long completedTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();

        stats.put("activeSchedules", activeSchedules);
        stats.put("totalMembers", totalMembers);
        stats.put("availableSlots", availableSlots);
        stats.put("completedTransactions", completedTransactions);

        return stats;
    }

    public List<Map<String, Object>> getEnrolledUsersForSchedule(long scheduleId) {
        List<ClassEnrollment> enrollments = classEnrollmentService.getAllEnrollments().stream()
                .filter(enrollment ->
                        enrollment.getSchedule() != null &&
                        enrollment.getSchedule().getId().equals(scheduleId) &&
                        enrollment.isPaid() &&
                        !Boolean.TRUE.equals(enrollment.getSessionCompleted()))
                .collect(Collectors.toList());

        return enrollments.stream()
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
    }

    public void markSessionAsCompleted(long scheduleId, long enrollmentId) {
        ClassEnrollment enrollment = classEnrollmentService.getEnrollmentById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        if (enrollment.getSchedule() == null) {
            throw new IllegalArgumentException("Enrollment does not belong to any schedule");
        }

        if (!enrollment.getSchedule().getId().equals(scheduleId)) {
            throw new IllegalArgumentException("Enrollment does not belong to this schedule");
        }

        if (Boolean.TRUE.equals(enrollment.getSessionCompleted())) {
            throw new IllegalArgumentException("Session already marked as completed");
        }

        enrollment.setSessionCompleted(true);
        classEnrollmentService.saveEnrollment(enrollment);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        if (schedule.getEnrolledCount() > 0) {
            schedule.setEnrolledCount(schedule.getEnrolledCount() - 1);
            scheduleRepository.save(schedule);
        }
    }

    public void cancelMembership(String transactionCode) {
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed memberships can be cancelled");
        }

        // Mark the transaction as cancelled (ensure CANCELLED exists in PaymentStatus enum)
        transaction.setPaymentStatus(PaymentStatus.CANCELLED);
        transactionRepository.save(transaction);

        
    }

}
