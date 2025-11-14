package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.ClassEntity;
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
    public ResponseEntity<ClassEntity> getClassById(@PathVariable Long id) {
        ClassEntity classEntity = classRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Class not found"));
        return ResponseEntity.ok(classEntity);
    }
}
