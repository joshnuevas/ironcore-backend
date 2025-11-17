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

    // Keep only these payment fields
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "total_amount")
    private Double totalAmount;
    
    @Column(name = "transaction_code")
    private String transactionCode;

    // REMOVED: payment_status, rating, feedback, attended_at

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
        
        // Copy values from transaction
        if (transaction != null) {
            this.paymentMethod = transaction.getPaymentMethod();
            this.totalAmount = transaction.getTotalAmount();
            this.transactionCode = transaction.getTransactionCode();
        }
    }

    // Helper methods - check payment status through transaction relationship
    public boolean isPaid() {
        return transaction != null && PaymentStatus.COMPLETED.equals(transaction.getPaymentStatus());
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
        // Update the copied fields
        if (transaction != null) {
            this.paymentMethod = transaction.getPaymentMethod();
            this.totalAmount = transaction.getTotalAmount();
            this.transactionCode = transaction.getTransactionCode();
        }
    }

    public Boolean getSessionCompleted() { return sessionCompleted; }
    public void setSessionCompleted(Boolean sessionCompleted) { this.sessionCompleted = sessionCompleted; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    // REMOVED: getPaymentStatus(), setPaymentStatus(), getRating(), setRating(), getFeedback(), setFeedback(), getAttendedAt(), setAttendedAt()
}