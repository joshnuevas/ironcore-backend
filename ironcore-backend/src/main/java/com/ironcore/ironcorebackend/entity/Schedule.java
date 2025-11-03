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
    @JsonIgnore  // ⭐ ADD THIS - Prevent circular reference
    private ClassEntity classEntity;

    private String day;

    private String timeSlot;

    private LocalDate date;

    private int slotsAvailable;

    @OneToMany(mappedBy = "schedule")
    @JsonIgnore  // ⭐ ADD THIS - Prevent circular reference
    private List<Transaction> transactions;

    // Keep all existing constructors, getters, and setters
    public Schedule() {}

    public Schedule(ClassEntity classEntity, String day, String timeSlot, LocalDate date, int slotsAvailable) {
        this.classEntity = classEntity;
        this.day = day;
        this.timeSlot = timeSlot;
        this.date = date;
        this.slotsAvailable = slotsAvailable;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getSlotsAvailable() { return slotsAvailable; }
    public void setSlotsAvailable(int slotsAvailable) { this.slotsAvailable = slotsAvailable; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}