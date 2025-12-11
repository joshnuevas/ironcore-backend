package com.ironcore.ironcorebackend.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public ScheduleController(ScheduleRepository scheduleRepository,
                              ClassRepository classRepository,
                              ClassEnrollmentRepository enrollmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Get all schedules with class info.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
        List<Schedule> schedules = scheduleRepository.findAll();

        List<Map<String, Object>> response = schedules.stream()
                .map(this::toScheduleResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get schedules by class ID.
     */
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Map<String, Object>>> getSchedulesByClass(@PathVariable long classId) {
        List<Schedule> schedules = scheduleRepository.findByClassEntity_Id(classId);

        List<Map<String, Object>> response = schedules.stream()
                .map(this::toScheduleResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single schedule by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        return ResponseEntity.ok(schedule);
    }

    /**
     * Create a new schedule.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> scheduleData) {
        long classId = Long.parseLong(scheduleData.get("classId").toString());
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Schedule schedule = new Schedule();
        schedule.setClassEntity(classEntity);
        schedule.setDay(scheduleData.get("day").toString());
        schedule.setTimeSlot(scheduleData.get("timeSlot").toString());
        schedule.setDate(LocalDate.parse(scheduleData.get("date").toString()));
        schedule.setMaxParticipants(((Number) scheduleData.get("maxParticipants")).intValue());
        schedule.setEnrolledCount(0);

        Schedule saved = scheduleRepository.save(schedule);

        Map<String, Object> response = toScheduleResponse(saved);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing schedule.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable long id,
            @RequestBody Schedule scheduleUpdate
    ) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setDay(scheduleUpdate.getDay());
        schedule.setTimeSlot(scheduleUpdate.getTimeSlot());
        schedule.setDate(scheduleUpdate.getDate());
        schedule.setMaxParticipants(scheduleUpdate.getMaxParticipants());

        Schedule updated = scheduleRepository.save(schedule);

        Map<String, Object> response = toScheduleResponse(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a schedule.
     * Throws RuntimeException if there are existing enrollments.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        if (schedule.getEnrolledCount() > 0) {
            throw new RuntimeException("Cannot delete schedule with existing enrollments");
        }

        // Delete all enrollment records linked to this schedule
        enrollmentRepository.deleteByScheduleId(id);

        // Now safe to delete schedule
        scheduleRepository.delete(schedule);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Schedule deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper to build the response map for a Schedule, including class info.
     */
    private Map<String, Object> toScheduleResponse(Schedule schedule) {
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
    }
}
