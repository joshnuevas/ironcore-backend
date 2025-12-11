package com.ironcore.ironcorebackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.repository.TrainerRepository;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerRepository trainerRepository;

    public TrainerController(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    /**
     * Get all trainers.
     */
    @GetMapping
    public ResponseEntity<List<Trainer>> getAll() {
        List<Trainer> trainers = trainerRepository.findAll();
        return ResponseEntity.ok(trainers);
    }

    /**
     * Get a single trainer by ID.
     * Returns null (same behavior as original), but wrapped in ResponseEntity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Trainer> getOne(@PathVariable long id) {
        Trainer trainer = trainerRepository.findById(id).orElse(null);
        return ResponseEntity.ok(trainer);
    }
}
