package com.ironcore.ironcorebackend.dto;

import java.time.LocalDateTime;

/**
 * DTO representing a user's membership status, including whether
 * they have an active or pending membership and detailed info about it.
 */
public class MembershipStatusResponse {

    private boolean hasActiveMembership;
    private boolean hasPendingMembership;

    private String membershipType;
    private String transactionCode;
    private LocalDateTime membershipActivatedDate;
    private LocalDateTime membershipExpiryDate;
    private String membershipStatus; // e.g., PENDING, COMPLETED, etc.

    public MembershipStatusResponse() {
        // Default constructor required for JSON serialization
    }

    public MembershipStatusResponse(
            boolean hasActiveMembership,
            boolean hasPendingMembership,
            String membershipType,
            String transactionCode,
            LocalDateTime membershipActivatedDate,
            LocalDateTime membershipExpiryDate,
            String membershipStatus
    ) {
        this.hasActiveMembership = hasActiveMembership;
        this.hasPendingMembership = hasPendingMembership;
        this.membershipType = membershipType;
        this.transactionCode = transactionCode;
        this.membershipActivatedDate = membershipActivatedDate;
        this.membershipExpiryDate = membershipExpiryDate;
        this.membershipStatus = membershipStatus;
    }

    // Getters & Setters

    public boolean isHasActiveMembership() {
        return hasActiveMembership;
    }

    public void setHasActiveMembership(boolean hasActiveMembership) {
        this.hasActiveMembership = hasActiveMembership;
    }

    public boolean isHasPendingMembership() {
        return hasPendingMembership;
    }

    public void setHasPendingMembership(boolean hasPendingMembership) {
        this.hasPendingMembership = hasPendingMembership;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
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

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus;
    }
}
