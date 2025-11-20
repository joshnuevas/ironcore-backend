package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "trainers")
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // what the trainer is known for (e.g. "HIIT & Cardio Specialist")
    private String specialty;

    // this matches the `location` column in your DB (NOT NULL)
    private String location;

    private int rating;
    private String image;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String experience;
    private String successRate;
    private String sessionsTaught;
    private String availability;

    @ElementCollection
    @CollectionTable(name = "trainer_certifications", joinColumns = @JoinColumn(name = "trainer_id"))
    private List<String> certifications;

    @ElementCollection
    @CollectionTable(name = "trainer_specializations", joinColumns = @JoinColumn(name = "trainer_id"))
    private List<String> specializations;

    // ============================
    // Constructors
    // ============================
    public Trainer() {}

    public Trainer(
            String name,
            String specialty,
            String location,
            int rating,
            String image,
            String description,
            String experience,
            String successRate,
            String sessionsTaught,
            String availability,
            List<String> certifications,
            List<String> specializations
    ) {
        this.name = name;
        this.specialty = specialty;
        this.location = location;
        this.rating = rating;
        this.image = image;
        this.description = description;
        this.experience = experience;
        this.successRate = successRate;
        this.sessionsTaught = sessionsTaught;
        this.availability = availability;
        this.certifications = certifications;
        this.specializations = specializations;
    }

    // ============================
    // Getters and Setters
    // ============================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getSuccessRate() { return successRate; }
    public void setSuccessRate(String successRate) { this.successRate = successRate; }

    public String getSessionsTaught() { return sessionsTaught; }
    public void setSessionsTaught(String sessionsTaught) { this.sessionsTaught = sessionsTaught; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<String> getSpecializations() { return specializations; }
    public void setSpecializations(List<String> specializations) { this.specializations = specializations; }
}
