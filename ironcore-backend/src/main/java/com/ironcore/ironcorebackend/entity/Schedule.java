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

    @Column(name = "class_name", nullable = true)
    private String className;

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

    @OneToMany(mappedBy = "schedule")
    @JsonIgnore
    private List<Transaction> transactions;

    public Schedule() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { 
        this.classEntity = classEntity;
        // ⭐ Auto-set className when classEntity is set
        if (classEntity != null) {
            this.className = classEntity.getName();
        }
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

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

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}