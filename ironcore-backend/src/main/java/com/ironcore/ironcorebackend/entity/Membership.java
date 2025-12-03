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

    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(name = "membership_type", nullable = false)
    private String membershipType;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "membership_activated_date")
    private LocalDateTime membershipActivatedDate;

    @Column(name = "membership_expiry_date")
    private LocalDateTime membershipExpiryDate;

    public Membership() {
    }

    public Membership(User user, Transaction transaction, String membershipType) {
        this.user = user;
        this.transaction = transaction;
        this.membershipType = membershipType;
        this.membershipActivatedDate = LocalDateTime.now();
        this.membershipExpiryDate = calculateExpiryDate(membershipType);
        this.transactionCode = transaction != null ? transaction.getTransactionCode() : null;
    }

    // ✅ Helper: is membership currently active (time-based)
    public boolean isCurrentlyActive() {
        if (membershipActivatedDate == null || membershipExpiryDate == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        // active if now >= activated AND now < expiry
        return !now.isBefore(membershipActivatedDate) && now.isBefore(membershipExpiryDate);
    }

    public boolean isExpired() {
        if (membershipExpiryDate == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(membershipExpiryDate);
    }

    // ✅ Calculate expiry based on your plan names
    private LocalDateTime calculateExpiryDate(String membershipType) {
        LocalDateTime now = LocalDateTime.now();

        if (membershipType == null) {
            return now.plusMonths(1); // safe default
        }

        switch (membershipType.toUpperCase()) {
            case "SESSION":
                // 1-day pass
                return now.plusDays(1);

            case "SILVER":
            case "GOLD":
            case "PLATINUM":
            case "MONTHLY":
                return now.plusMonths(1);

            case "QUARTERLY":
                return now.plusMonths(3);

            case "ANNUAL":
                return now.plusYears(1);

            default:
                // unknown type → default to 1 month
                return now.plusMonths(1);
        }
    }

    // Convenience method to access payment status via transaction
    public PaymentStatus getPaymentStatus() {
        return transaction != null ? transaction.getPaymentStatus() : null;
    }

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
