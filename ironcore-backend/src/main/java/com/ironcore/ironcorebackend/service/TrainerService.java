package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.repository.TrainerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TrainerService {

    private final TrainerRepository trainerRepository;

    public TrainerService(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    public List<Trainer> getAllTrainers() {
        return trainerRepository.findAll();
    }

    public Optional<Trainer> getTrainerById(Long id) {
        // Fix: enforce @NonNull parameter
        return trainerRepository.findById(Objects.requireNonNull(id));
    }

    public Trainer saveTrainer(Trainer trainer) {
        // Fix: enforce @NonNull entity before saving
        return trainerRepository.save(Objects.requireNonNull(trainer));
    }
}
