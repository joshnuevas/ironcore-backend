package com.ironcore.ironcorebackend.dto;

import java.time.LocalDateTime;

public class MembershipStatusResponse {

    private boolean hasActiveMembership;
    private boolean hasPendingMembership;

    private String membershipType;
    private String transactionCode;
    private LocalDateTime membershipActivatedDate;
    private LocalDateTime membershipExpiryDate;
    private String membershipStatus; // PENDING, COMPLETED, etc.

    public boolean isHasActiveMembership() { return hasActiveMembership; }
    public void setHasActiveMembership(boolean hasActiveMembership) { this.hasActiveMembership = hasActiveMembership; }

    public boolean isHasPendingMembership() { return hasPendingMembership; }
    public void setHasPendingMembership(boolean hasPendingMembership) { this.hasPendingMembership = hasPendingMembership; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public LocalDateTime getMembershipActivatedDate() { return membershipActivatedDate; }
    public void setMembershipActivatedDate(LocalDateTime membershipActivatedDate) { this.membershipActivatedDate = membershipActivatedDate; }

    public LocalDateTime getMembershipExpiryDate() { return membershipExpiryDate; }
    public void setMembershipExpiryDate(LocalDateTime membershipExpiryDate) { this.membershipExpiryDate = membershipExpiryDate; }

    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }
}
