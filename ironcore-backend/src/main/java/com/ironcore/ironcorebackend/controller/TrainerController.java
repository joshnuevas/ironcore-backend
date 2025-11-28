package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.repository.TrainerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerRepository trainerRepository;

    public TrainerController(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @GetMapping
    public List<Trainer> getAll() {
        return trainerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Trainer getOne(@PathVariable long id) {
        return trainerRepository.findById(id).orElse(null);
    }
}
