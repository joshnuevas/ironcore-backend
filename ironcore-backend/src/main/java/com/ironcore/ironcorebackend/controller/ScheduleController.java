package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;

    public ScheduleController(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // Get all schedules
    @GetMapping
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleRepository.findAll());
    }

    // Get schedules by class ID
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Schedule>> getSchedulesByClass(@PathVariable Long classId) {
        List<Schedule> schedules = scheduleRepository.findByClassEntity_Id(classId);
        return ResponseEntity.ok(schedules);
    }

    // Get a single schedule by ID
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        return ResponseEntity.ok(schedule);
    }
}