package com.ironcore.ironcorebackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    @JsonIgnore
    private ClassEntity classEntity;

    @Column(nullable = false)
    private String day;

    @Column(name = "time_slot", nullable = false)
    private String timeSlot;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount = 0;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 15;

    // REMOVED: Direct transaction relationship - Transactions are financial records
    // @OneToMany(mappedBy = "schedule")
    // @JsonIgnore
    // private List<Transaction> transactions;

    // ADDED: Relationship to ClassEnrollment (proper domain entity)
    @OneToMany(mappedBy = "schedule")
    @JsonIgnore
    private List<ClassEnrollment> enrollments;

    public Schedule() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { 
        this.classEntity = classEntity;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(Integer enrolledCount) { this.enrolledCount = enrolledCount; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    // REMOVED: Transactions getter/setter
    // public List<Transaction> getTransactions() { return transactions; }
    // public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    // ADDED: Enrollments getter/setter
    public List<ClassEnrollment> getEnrollments() { return enrollments; }
    public void setEnrollments(List<ClassEnrollment> enrollments) { this.enrollments = enrollments; }

    // Helper methods for business logic
    public boolean hasAvailableSlots() {
        return enrolledCount < maxParticipants;
    }

    public int getAvailableSlots() {
        return maxParticipants - enrolledCount;
    }

    public boolean isFullyBooked() {
        return enrolledCount >= maxParticipants;
    }

    public void incrementEnrolledCount() {
        if (this.enrolledCount == null) {
            this.enrolledCount = 0;
        }
        this.enrolledCount++;
    }

    public void decrementEnrolledCount() {
        if (this.enrolledCount != null && this.enrolledCount > 0) {
            this.enrolledCount--;
        }
    }

    // Helper to get active enrollment count
    public long getActiveEnrollmentCount() {
        if (enrollments == null) return 0;
        return enrollments.stream()
                .filter(enrollment -> enrollment.isPaid() && !enrollment.getSessionCompleted())
                .count();
    }
}