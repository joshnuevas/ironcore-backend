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
    private double price;

    // Extra info to fully power ClassDetailsPage from DB
    private String intensity;          // e.g. "High", "Medium"
    private Integer maxParticipants;   // e.g. 15
    private String location;           // e.g. "Main Studio Floor"
    private String cancelPolicy;       // e.g. "You can cancel up to 2 hours before..."
    private String imageUrl;           // e.g. "/images/hiit.png" or full URL

    // Link to Trainer (you already have Trainer entity)
    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @OneToMany(mappedBy = "classEntity")
    @JsonIgnore
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "classEntity")
    @JsonIgnore
    private List<ClassEnrollment> enrollments;

    // Dynamic lists instead of hardcoded arrays in React
    @ElementCollection
    @CollectionTable(name = "class_expectations", joinColumns = @JoinColumn(name = "class_id"))
    @Column(name = "text")
    private List<String> expectations;

    @ElementCollection
    @CollectionTable(name = "class_benefits", joinColumns = @JoinColumn(name = "class_id"))
    @Column(name = "text")
    private List<String> benefits;

    @ElementCollection
    @CollectionTable(name = "class_requirements", joinColumns = @JoinColumn(name = "class_id"))
    @Column(name = "text")
    private List<String> requirements;

    // ============================
    // Constructors
    // ============================

    public ClassEntity() {
    }

    public ClassEntity(String name, String icon, String description, String duration, double price) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.duration = duration;
        this.price = price;
    }

    // ============================
    // Getters and Setters
    // ============================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCancelPolicy() {
        return cancelPolicy;
    }

    public void setCancelPolicy(String cancelPolicy) {
        this.cancelPolicy = cancelPolicy;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public List<ClassEnrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<ClassEnrollment> enrollments) {
        this.enrollments = enrollments;
    }

    public List<String> getExpectations() {
        return expectations;
    }

    public void setExpectations(List<String> expectations) {
        this.expectations = expectations;
    }

    public List<String> getBenefits() {
        return benefits;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }

    // ============================
    // Helper Methods
    // ============================

    public long getActiveEnrollmentCount() {
        if (enrollments == null) return 0;
        return enrollments.stream()
                .filter(enrollment -> enrollment.isPaid() && !enrollment.getSessionCompleted())
                .count();
    }
}
