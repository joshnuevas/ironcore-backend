package com.ironcore.ironcorebackend.dto;

import java.time.LocalDateTime;

public class TransactionRequest {

    private Long userId;

    // For memberships
    private String membershipType;
    private LocalDateTime membershipActivatedDate;
    private LocalDateTime membershipExpiryDate;

    // For class transactions
    private Long scheduleId;

    // Payment info
    private Double totalAmount;
    private Double processingFee;
    private String paymentMethod;
    private String paymentStatus; // Use string, converted to enum in service

    // Constructors
    public TransactionRequest() {}

    public TransactionRequest(Long userId, String membershipType, LocalDateTime membershipActivatedDate,
                              LocalDateTime membershipExpiryDate, Long scheduleId, Double totalAmount,
                              Double processingFee, String paymentMethod, String paymentStatus) {
        this.userId = userId;
        this.membershipType = membershipType;
        this.membershipActivatedDate = membershipActivatedDate;
        this.membershipExpiryDate = membershipExpiryDate;
        this.scheduleId = scheduleId;
        this.totalAmount = totalAmount;
        this.processingFee = processingFee;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public LocalDateTime getMembershipActivatedDate() { return membershipActivatedDate; }
    public void setMembershipActivatedDate(LocalDateTime membershipActivatedDate) { this.membershipActivatedDate = membershipActivatedDate; }

    public LocalDateTime getMembershipExpiryDate() { return membershipExpiryDate; }
    public void setMembershipExpiryDate(LocalDateTime membershipExpiryDate) { this.membershipExpiryDate = membershipExpiryDate; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getProcessingFee() { return processingFee; }
    public void setProcessingFee(Double processingFee) { this.processingFee = processingFee; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
