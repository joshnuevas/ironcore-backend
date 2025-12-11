package com.ironcore.ironcorebackend.dto;

import java.util.List;

public class TrainerDetailsDto {

    private Long id;
    private String name;
    private String image;          // URL or emoji
    private String specialty;
    private String location;
    private String description;
    private String experience;
    private String successRate;
    private String sessionsTaught;
    private String availability;

    private Integer rating;        // Wrapper → nullable
    private List<String> certifications;
    private List<String> specializations;

    public TrainerDetailsDto() {
        // Required default constructor
    }

    public TrainerDetailsDto(
            Long id,
            String name,
            String image,
            String specialty,
            String location,
            String description,
            String experience,
            String successRate,
            String sessionsTaught,
            String availability,
            Integer rating,
            List<String> certifications,
            List<String> specializations
    ) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.specialty = specialty;
        this.location = location;
        this.description = description;
        this.experience = experience;
        this.successRate = successRate;
        this.sessionsTaught = sessionsTaught;
        this.availability = availability;
        this.rating = rating;
        this.certifications = certifications;
        this.specializations = specializations;
    }

    // Getters and setters

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(String successRate) {
        this.successRate = successRate;
    }

    public String getSessionsTaught() {
        return sessionsTaught;
    }

    public void setSessionsTaught(String sessionsTaught) {
        this.sessionsTaught = sessionsTaught;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public List<String> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations;
    }
}
