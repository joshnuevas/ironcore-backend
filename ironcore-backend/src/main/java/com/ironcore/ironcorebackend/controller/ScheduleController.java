package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;

    public ScheduleController(ScheduleRepository scheduleRepository, ClassRepository classRepository) {
        this.scheduleRepository = scheduleRepository;
        this.classRepository = classRepository;
    }

    // Get all schedules with class info
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
        List<Schedule> schedules = scheduleRepository.findAll();
        
        List<Map<String, Object>> response = schedules.stream()
            .map(schedule -> {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("day", schedule.getDay());
                scheduleMap.put("timeSlot", schedule.getTimeSlot());
                scheduleMap.put("date", schedule.getDate());
                scheduleMap.put("enrolledCount", schedule.getEnrolledCount());
                scheduleMap.put("maxParticipants", schedule.getMaxParticipants());
                
                // Add class entity info
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("id", schedule.getClassEntity().getId());
                classInfo.put("name", schedule.getClassEntity().getName());
                scheduleMap.put("classEntity", classInfo);
                
                return scheduleMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    // Get schedules by class ID
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Map<String, Object>>> getSchedulesByClass(@PathVariable Long classId) {
        List<Schedule> schedules = scheduleRepository.findByClassEntity_Id(classId);
        
        List<Map<String, Object>> response = schedules.stream()
            .map(schedule -> {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("day", schedule.getDay());
                scheduleMap.put("timeSlot", schedule.getTimeSlot());
                scheduleMap.put("date", schedule.getDate());
                scheduleMap.put("enrolledCount", schedule.getEnrolledCount());
                scheduleMap.put("maxParticipants", schedule.getMaxParticipants());
                
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("id", schedule.getClassEntity().getId());
                classInfo.put("name", schedule.getClassEntity().getName());
                scheduleMap.put("classEntity", classInfo);
                
                return scheduleMap;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    // Get a single schedule by ID
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        return ResponseEntity.ok(schedule);
    }

    // Create new schedule
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> scheduleData) {
        Long classId = Long.valueOf(scheduleData.get("classId").toString());
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        
        Schedule schedule = new Schedule();
        schedule.setClassEntity(classEntity);
        schedule.setClassName(classEntity.getName()); // ⭐ Automatically set className
        schedule.setDay(scheduleData.get("day").toString());
        schedule.setTimeSlot(scheduleData.get("timeSlot").toString());
        schedule.setDate(LocalDate.parse(scheduleData.get("date").toString()));
        schedule.setMaxParticipants(Integer.parseInt(scheduleData.get("maxParticipants").toString()));
        schedule.setEnrolledCount(0);
        
        Schedule saved = scheduleRepository.save(schedule);
        
        // Return with class info
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("day", saved.getDay());
        response.put("timeSlot", saved.getTimeSlot());
        response.put("date", saved.getDate());
        response.put("enrolledCount", saved.getEnrolledCount());
        response.put("maxParticipants", saved.getMaxParticipants());
        
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("id", saved.getClassEntity().getId());
        classInfo.put("name", saved.getClassEntity().getName());
        response.put("classEntity", classInfo);
        
        return ResponseEntity.ok(response);
    }

    // Update schedule
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Long id, 
            @RequestBody Schedule scheduleUpdate) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        schedule.setDay(scheduleUpdate.getDay());
        schedule.setTimeSlot(scheduleUpdate.getTimeSlot());
        schedule.setDate(scheduleUpdate.getDate());
        schedule.setMaxParticipants(scheduleUpdate.getMaxParticipants());
        
        Schedule updated = scheduleRepository.save(schedule);
        
        // Return with class info
        Map<String, Object> response = new HashMap<>();
        response.put("id", updated.getId());
        response.put("day", updated.getDay());
        response.put("timeSlot", updated.getTimeSlot());
        response.put("date", updated.getDate());
        response.put("enrolledCount", updated.getEnrolledCount());
        response.put("maxParticipants", updated.getMaxParticipants());
        
        Map<String, Object> classInfo = new HashMap<>();
        classInfo.put("id", updated.getClassEntity().getId());
        classInfo.put("name", updated.getClassEntity().getName());
        response.put("classEntity", classInfo);
        
        return ResponseEntity.ok(response);
    }

    // Delete schedule
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        // Check if schedule has enrollments
        if (schedule.getEnrolledCount() > 0) {
            throw new RuntimeException("Cannot delete schedule with existing enrollments");
        }
        
        scheduleRepository.delete(schedule);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Schedule deleted successfully");
        return ResponseEntity.ok(response);
    }
}