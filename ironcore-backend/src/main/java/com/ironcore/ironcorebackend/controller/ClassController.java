package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.dto.ClassDetailsDto;
import com.ironcore.ironcorebackend.dto.TrainerDetailsDto;
import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ClassController {

    private final ClassRepository classRepository;

    public ClassController(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @GetMapping
    public ResponseEntity<List<ClassEntity>> getAllClasses() {
        return ResponseEntity.ok(classRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassEntity> getClassById(@PathVariable long id) {
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        return ResponseEntity.ok(classEntity);
    }

    // Full class details for ClassDetailsPage
    @GetMapping("/{id}/details")
    public ResponseEntity<ClassDetailsDto> getClassDetails(@PathVariable long id) {
        ClassEntity cls = classRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        ClassDetailsDto dto = new ClassDetailsDto();
        dto.setId(cls.getId());
        dto.setName(cls.getName());
        dto.setIcon(cls.getIcon());
        dto.setDescription(cls.getDescription());
        dto.setPrice(cls.getPrice());
        dto.setDuration(cls.getDuration());
        dto.setIntensity(cls.getIntensity());
        dto.setMaxParticipants(cls.getMaxParticipants());
        dto.setLocation(cls.getLocation());
        dto.setCancelPolicy(cls.getCancelPolicy());
        dto.setImageUrl(cls.getImageUrl());
        dto.setExpectations(cls.getExpectations());
        dto.setBenefits(cls.getBenefits());
        dto.setRequirements(cls.getRequirements());

        Trainer trainer = cls.getTrainer();
        if (trainer != null) {
            TrainerDetailsDto t = new TrainerDetailsDto();
            t.setId(trainer.getId());
            t.setName(trainer.getName());
            t.setImage(trainer.getImage());
            t.setSpecialty(trainer.getSpecialty());
            t.setLocation(trainer.getLocation());
            t.setDescription(trainer.getDescription());
            t.setExperience(trainer.getExperience());
            t.setSuccessRate(trainer.getSuccessRate());
            t.setSessionsTaught(trainer.getSessionsTaught());
            t.setAvailability(trainer.getAvailability());
            t.setRating(trainer.getRating());
            t.setCertifications(trainer.getCertifications());
            t.setSpecializations(trainer.getSpecializations());

            dto.setTrainer(t);
        }

        return ResponseEntity.ok(dto);
    }
}
