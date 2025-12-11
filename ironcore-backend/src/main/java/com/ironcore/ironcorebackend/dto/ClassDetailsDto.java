package com.ironcore.ironcorebackend.dto;

import java.util.List;

/**
 * DTO representing full details of a class,
 * including trainer info and rich content (expectations, benefits, requirements).
 */
public class ClassDetailsDto {

    private Long id;
    private String name;
    private String icon;
    private String description;
    private double price;
    private String duration;
    private String intensity;
    private Integer maxParticipants;
    private String location;
    private String cancelPolicy;
    private String imageUrl;

    private TrainerDetailsDto trainer;

    private List<String> expectations;
    private List<String> benefits;
    private List<String> requirements;

    public ClassDetailsDto() {
        // Default constructor for frameworks (Jackson, etc.)
    }

    public ClassDetailsDto(
            Long id,
            String name,
            String icon,
            String description,
            double price,
            String duration,
            String intensity,
            Integer maxParticipants,
            String location,
            String cancelPolicy,
            String imageUrl,
            TrainerDetailsDto trainer,
            List<String> expectations,
            List<String> benefits,
            List<String> requirements
    ) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.intensity = intensity;
        this.maxParticipants = maxParticipants;
        this.location = location;
        this.cancelPolicy = cancelPolicy;
        this.imageUrl = imageUrl;
        this.trainer = trainer;
        this.expectations = expectations;
        this.benefits = benefits;
        this.requirements = requirements;
    }

    // Getters & Setters

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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
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

    public TrainerDetailsDto getTrainer() {
        return trainer;
    }

    public void setTrainer(TrainerDetailsDto trainer) {
        this.trainer = trainer;
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
}
