package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "class_enrollments")
public class ClassEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code")
    private String transactionCode;

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

    @Column(name = "session_completed")
    private Boolean sessionCompleted = false;

    // ================================
    // Constructors
    // ================================

    public ClassEnrollment() {}

    public ClassEnrollment(User user, ClassEntity classEntity, Schedule schedule, Transaction transaction) {
        this.user = user;
        this.classEntity = classEntity;
        this.schedule = schedule;
        setTransaction(transaction); // ensures transactionCode is stored
        this.sessionCompleted = false;
    }

    // ================================
    // Domain Logic
    // ================================

    public boolean isPaid() {
        return transaction != null &&
               PaymentStatus.COMPLETED.equals(transaction.getPaymentStatus());
    }

    public String getPaymentMethod() {
        return transaction != null ? transaction.getPaymentMethod() : null;
    }

    public Double getTotalAmount() {
        return transaction != null ? transaction.getTotalAmount() : null;
    }

    // ================================
    // Getters and Setters
    // ================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
        // Copy transaction code into its own column
        if (transaction != null) {
            this.transactionCode = transaction.getTransactionCode();
        }
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Boolean getSessionCompleted() {
        return sessionCompleted;
    }

    public void setSessionCompleted(Boolean sessionCompleted) {
        this.sessionCompleted = sessionCompleted;
    }
}