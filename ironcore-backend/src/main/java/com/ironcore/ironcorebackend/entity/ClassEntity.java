package com.ironcore.ironcorebackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String icon;

    private String description;

    private String duration;

    private int maxParticipants;

    private double price;

    // Relationships
    @OneToMany(mappedBy = "classEntity")
    @JsonIgnore  // ⭐ ADD THIS - Prevent circular reference
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "classEntity")
    @JsonIgnore  // ⭐ ADD THIS - Prevent circular reference
    private List<Transaction> transactions;

    // Constructors
    public ClassEntity() {}

    public ClassEntity(String name, String icon, String description, String duration, int maxParticipants, double price) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.duration = duration;
        this.maxParticipants = maxParticipants;
        this.price = price;
    }

    // Getters and Setters (keep all existing ones)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<Schedule> getSchedules() { return schedules; }
    public void setSchedules(List<Schedule> schedules) { this.schedules = schedules; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}