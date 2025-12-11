package com.ironcore.ironcorebackend.dto;

import java.time.LocalDateTime;

/**
 * DTO used when the frontend creates a transaction.
 * Can represent BOTH:
 *  - Membership purchases
 *  - Class enrollments
 *
 * All fields are optional depending on transaction type.
 */
public class TransactionRequest {

    // User making the transaction
    private Long userId;

    // Membership fields
    private String membershipType;
    private LocalDateTime membershipActivatedDate;
    private LocalDateTime membershipExpiryDate;

    // Class-related fields
    private Long scheduleId;
    private Long classId;

    // Payment fields
    private Double totalAmount;
    private Double processingFee;
    private String paymentMethod;
    private String paymentStatus; // String → converted to enum in service layer

    // Default constructor (required for JSON deserialization)
    public TransactionRequest() {}

    // Full constructor
    public TransactionRequest(
            Long userId,
            String membershipType,
            LocalDateTime membershipActivatedDate,
            LocalDateTime membershipExpiryDate,
            Long scheduleId,
            Long classId,
            Double totalAmount,
            Double processingFee,
            String paymentMethod,
            String paymentStatus
    ) {
        this.userId = userId;
        this.membershipType = membershipType;
        this.membershipActivatedDate = membershipActivatedDate;
        this.membershipExpiryDate = membershipExpiryDate;
        this.scheduleId = scheduleId;
        this.classId = classId;
        this.totalAmount = totalAmount;
        this.processingFee = processingFee;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    // Getters & Setters

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMembershipType() {
        return membershipType;
    }
    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public LocalDateTime getMembershipActivatedDate() {
        return membershipActivatedDate;
    }
    public void setMembershipActivatedDate(LocalDateTime membershipActivatedDate) {
        this.membershipActivatedDate = membershipActivatedDate;
    }

    public LocalDateTime getMembershipExpiryDate() {
        return membershipExpiryDate;
    }
    public void setMembershipExpiryDate(LocalDateTime membershipExpiryDate) {
        this.membershipExpiryDate = membershipExpiryDate;
    }

    public Long getScheduleId() {
        return scheduleId;
    }
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getClassId() {
        return classId;
    }
    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getProcessingFee() {
        return processingFee;
    }
    public void setProcessingFee(Double processingFee) {
        this.processingFee = processingFee;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
