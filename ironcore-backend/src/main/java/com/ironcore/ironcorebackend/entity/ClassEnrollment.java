package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_enrollments")
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    private Boolean sessionCompleted = false;

    // No-arg constructor
    public ClassEnrollment() {
    }

    // Constructor
    public ClassEnrollment(User user, ClassEntity classEntity, Schedule schedule, Transaction transaction) {
        this.user = user;
        this.classEntity = classEntity;
        this.schedule = schedule;
        this.transaction = transaction;
        this.sessionCompleted = false;
        // ⭐ No need to copy payment fields anymore
    }

    // Helper methods - check payment status through transaction relationship
    public boolean isPaid() {
        return transaction != null && PaymentStatus.COMPLETED.equals(transaction.getPaymentStatus());
    }

    // ⭐ Helper method to get payment method from transaction
    public String getPaymentMethod() {
        return transaction != null ? transaction.getPaymentMethod() : null;
    }

    // ⭐ Helper method to get total amount from transaction
    public Double getTotalAmount() {
        return transaction != null ? transaction.getTotalAmount() : null;
    }

    // ⭐ Helper method to get transaction code from transaction
    public String getTransactionCode() {
        return transaction != null ? transaction.getTransactionCode() : null;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { 
        this.transaction = transaction;
        // ⭐ No need to copy fields anymore
    }

    public Boolean getSessionCompleted() { return sessionCompleted; }
    public void setSessionCompleted(Boolean sessionCompleted) { this.sessionCompleted = sessionCompleted; }
}