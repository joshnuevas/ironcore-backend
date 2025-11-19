package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    // Membership-specific data only
    private String membershipType;

    // KEEP only transaction_code
    @Column(name = "transaction_code")
    private String transactionCode;

    // ADD these date fields since we removed the duplicates
    @Column(name = "membership_activated_date")
    private LocalDateTime membershipActivatedDate;
    
    @Column(name = "membership_expiry_date")
    private LocalDateTime membershipExpiryDate;

    public Membership() {}

    // Updated constructor
    public Membership(User user, Transaction transaction, String membershipType) {
        this.user = user;
        this.transaction = transaction;
        this.membershipType = membershipType;
        this.membershipActivatedDate = LocalDateTime.now();
        this.membershipExpiryDate = calculateExpiryDate(membershipType);
        this.transactionCode = transaction != null ? transaction.getTransactionCode() : null;
    }

    // Helper methods
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return membershipActivatedDate != null && 
               membershipExpiryDate != null && 
               now.isAfter(membershipActivatedDate) && 
               now.isBefore(membershipExpiryDate);
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return membershipExpiryDate != null && now.isAfter(membershipExpiryDate);
    }

    // Calculate expiry date based on membership type
    private LocalDateTime calculateExpiryDate(String membershipType) {
        LocalDateTime now = LocalDateTime.now();
        switch (membershipType.toUpperCase()) {
            case "MONTHLY":
                return now.plusMonths(1);
            case "QUARTERLY":
                return now.plusMonths(3);
            case "ANNUAL":
                return now.plusYears(1);
            default:
                return now.plusMonths(1); // default to monthly
        }
    }

    // Convenience method to access payment status via transaction
    public PaymentStatus getPaymentStatus() {
        return transaction != null ? transaction.getPaymentStatus() : null;
    }

    // FIXED: Use getPaymentDate() which exists in Transaction
    public LocalDateTime getTransactionPaymentDate() {
        return transaction != null ? transaction.getPaymentDate() : null;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { 
        this.transaction = transaction;
        if (transaction != null) {
            this.transactionCode = transaction.getTransactionCode();
        }
    }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { 
        this.membershipType = membershipType;
        // Update expiry date when membership type changes
        if (this.membershipActivatedDate != null) {
            this.membershipExpiryDate = calculateExpiryDate(membershipType);
        }
    }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public LocalDateTime getMembershipActivatedDate() { return membershipActivatedDate; }
    public void setMembershipActivatedDate(LocalDateTime membershipActivatedDate) { 
        this.membershipActivatedDate = membershipActivatedDate;
    }

    public LocalDateTime getMembershipExpiryDate() { return membershipExpiryDate; }
    public void setMembershipExpiryDate(LocalDateTime membershipExpiryDate) { 
        this.membershipExpiryDate = membershipExpiryDate;
    }
}