package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ⭐ ADD THIS: Store user email at time of transaction
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = true)
    private ClassEntity classEntity;

    // ⭐ ADD THIS: Store class name at time of transaction
    @Column(name = "class_name", nullable = true)
    private String className;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = true)
    private Schedule schedule;

    @Column(name = "membership_type", nullable = true)
    private String membershipType;

    private double processingFee = 20;

    private double totalAmount;

    private String paymentMethod = "GCash";

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime paymentDate = LocalDateTime.now();

    // Constructors
    public Transaction() {}

    public Transaction(User user, ClassEntity classEntity, Schedule schedule, double totalAmount) {
        this.user = user;
        this.classEntity = classEntity;
        this.schedule = schedule;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // ⭐ ADD GETTER AND SETTER
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    // ⭐ ADD GETTER AND SETTER
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public double getProcessingFee() { return processingFee; }
    public void setProcessingFee(double processingFee) { this.processingFee = processingFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}