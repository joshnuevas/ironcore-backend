package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.stereotype.Service;
import com.ironcore.ironcorebackend.entity.PaymentStatus;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ScheduleRepository scheduleRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            ClassRepository classRepository,
            ScheduleRepository scheduleRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Transaction createTransactionFromRequest(TransactionRequest request) {
        // Fetch the entities by ID
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
        
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        // Create the transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setClassEntity(classEntity);
        transaction.setSchedule(schedule);
        transaction.setProcessingFee(request.getProcessingFee());
        transaction.setTotalAmount(request.getTotalAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setPaymentStatus(PaymentStatus.valueOf(request.getPaymentStatus()));
        transaction.setPaymentDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}