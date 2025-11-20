package com.ironcore.ironcorebackend.config;

import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.repository.TrainerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TrainerSeeder implements CommandLineRunner {

    private final TrainerRepository trainerRepository;

    public TrainerSeeder(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Override
    public void run(String... args) {

        if (trainerRepository.count() == 0) {

            trainerRepository.save(new Trainer(
                    "Coach Sarah Martinez",
                    "HIIT & Cardio Specialist",      // specialty
                    "HIIT & Cardio Specialist",      // location (for UI)
                    5,
                    "sarah_martinez.png",
                    "With 8 years of experience, Coach Sarah specializes in high-intensity interval training and cardio workouts. Her energetic approach and focus on fat burning and endurance makes her classes challenging yet rewarding for all fitness levels.",
                    "8 years",
                    "95%",
                    "2,400+",
                    "Mon–Sat, 6 AM–8 PM",
                    Arrays.asList("NASM CPT", "HIIT Specialist", "Nutrition Coach L1", "CPR Certified"),
                    Arrays.asList("HIIT", "Fat Loss", "Endurance", "Conditioning")
            ));

            trainerRepository.save(new Trainer(
                    "Coach Maria Santos",
                    "Dance & Cardio",
                    "Dance & Cardio",
                    5,
                    "maria_santos.png",
                    "Coach Maria brings 7 years of dance and fitness expertise to every Zumba session. Her infectious energy and passion for dance-based cardio creates a fun, welcoming environment where everyone feels confident moving to the beat.",
                    "7 years",
                    "98%",
                    "3,200+",
                    "Mon–Sat, 8 AM–7 PM",
                    Arrays.asList("Zumba Instructor", "AFAA Group Fitness", "Dance Therapy", "CPR Certified"),
                    Arrays.asList("Zumba", "Dance Fitness", "Cardio Dance", "Rhythmic Training")
            ));

            trainerRepository.save(new Trainer(
                    "Coach Anna Lee",
                    "Cardio & Endurance",
                    "Cardio & Endurance",
                    5,
                    "anna_lee.png",
                    "With 6 years of experience in cycling and endurance training, Coach Anna leads high-energy spin classes that build stamina and leg strength. Her motivating coaching style pushes you to reach new performance levels.",
                    "6 years",
                    "93%",
                    "1,800+",
                    "Mon–Sat, 7 AM–6 PM",
                    Arrays.asList("Spinning Instructor", "ACE Trainer", "Endurance Specialist", "CPR Certified"),
                    Arrays.asList("Cycling", "Endurance", "Leg Strength", "Speed Training")
            ));

            trainerRepository.save(new Trainer(
                    "Coach Linda Chen",
                    "Yoga & Meditation",
                    "Yoga & Meditation",
                    5,
                    "linda_chen.png",
                    "A master yoga instructor with 10 years of experience, Coach Linda guides students through mindful movement practices that build strength, flexibility, and inner peace. Her calming presence and expert instruction create transformative yoga experiences.",
                    "10 years",
                    "99%",
                    "4,500+",
                    "Mon–Sat, 9 AM–5 PM",
                    Arrays.asList("RYT-500", "Meditation Instructor", "Ayurvedic Coach", "CPR Certified"),
                    Arrays.asList("Yoga", "Meditation", "Flexibility", "Mobility")
            ));

            trainerRepository.save(new Trainer(
                    "Coach Emily Rodriguez",
                    "Pilates & Core Training",
                    "Pilates & Core Training",
                    5,
                    "emily_rodriguez.png",
                    "Coach Emily has dedicated 9 years to mastering Pilates and core strengthening techniques. Her precise instruction and focus on proper alignment helps clients develop a strong, stable core while improving overall flexibility and posture.",
                    "9 years",
                    "96%",
                    "3,600+",
                    "Mon–Sat, 7 AM–7 PM",
                    Arrays.asList("PMA Pilates Instructor", "Reformer Specialist", "Posture Specialist", "CPR Certified"),
                    Arrays.asList("Pilates", "Core", "Posture", "Rehab")
            ));

            trainerRepository.save(new Trainer(
                    "Coach Mark Johnson",
                    "Boxing & Combat Fitness",
                    "Boxing & Combat Fitness",
                    5,
                    "mark_johnson.png",
                    "With 11 years in boxing and combat fitness, Coach Mark delivers powerful, high-energy workouts that combine cardio, strength training, and stress relief. His expertise makes boxing accessible and exciting for all experience levels.",
                    "11 years",
                    "94%",
                    "4,200+",
                    "Mon–Sat, 6 AM–9 PM",
                    Arrays.asList("USA Boxing Coach", "NASM PES", "CSCS", "CPR Certified"),
                    Arrays.asList("Boxing", "Combat", "Power", "Agility")
            ));
        }
    }
}
