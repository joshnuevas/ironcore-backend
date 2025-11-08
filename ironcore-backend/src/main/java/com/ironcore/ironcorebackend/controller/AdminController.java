package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.TransactionRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminController {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AdminController(
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count active schedules
        long activeSchedules = scheduleRepository.count();

        // Count total members (users)
        long totalMembers = userRepository.count();

        // Calculate available slots
        long availableSlots = scheduleRepository.findAll().stream()
                .mapToLong(schedule -> schedule.getMaxParticipants() - schedule.getEnrolledCount())
                .sum();

        // Count completed transactions
        long completedTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .count();

        stats.put("activeSchedules", activeSchedules);
        stats.put("totalMembers", totalMembers);
        stats.put("availableSlots", availableSlots);
        stats.put("completedTransactions", completedTransactions);

        return ResponseEntity.ok(stats);
    }
}