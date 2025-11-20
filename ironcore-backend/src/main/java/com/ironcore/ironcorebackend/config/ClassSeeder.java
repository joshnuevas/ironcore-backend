package com.ironcore.ironcorebackend.config;

import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ClassSeeder implements CommandLineRunner {

    private final ClassRepository classRepository;

    public ClassSeeder(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @Override
    public void run(String... args) {

        seedClassIfNotExists(
                "HIIT",
                "🔥",
                "Short bursts of high-intensity exercise followed by rest. Great for fat burning and endurance.",
                "45 mins",
                500.0
        );

        seedClassIfNotExists(
                "ZUMBA",
                "💃",
                "Dance-based cardio workout set to music. Fun, energetic, and great for all fitness levels.",
                "60 mins",
                400.0
        );

        seedClassIfNotExists(
                "SPIN",
                "🚴",
                "High-energy cycling workout focusing on stamina, leg strength, and endurance.",
                "45 mins",
                450.0
        );

        seedClassIfNotExists(
                "YOGA",
                "🧘",
                "Mindful movement to build strength, flexibility, and balance while relieving stress.",
                "60 mins",
                400.0
        );

        seedClassIfNotExists(
                "PILATES",
                "💪",
                "Low-impact exercises that strengthen the core and improve postural alignment and flexibility.",
                "50 mins",
                450.0
        );

        seedClassIfNotExists(
                "BOXING",
                "🥊",
                "High-energy boxing workout combining cardio, strength training, and stress relief. No experience needed.",
                "55 mins",
                500.0
        );
    }

    private void seedClassIfNotExists(String name,
                                      String icon,
                                      String description,
                                      String duration,
                                      double price) {

        boolean exists = classRepository.existsByName(name);
        if (exists) {
            return; // ✅ Skip if already in DB
        }

        ClassEntity classEntity = new ClassEntity(
                name,
                icon,
                description,
                duration,
                price
        );

        classRepository.save(classEntity);
    }
}
