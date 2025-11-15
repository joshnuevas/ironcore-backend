package com.ironcore.ironcorebackend.repository;

import java.util.List;
import com.ironcore.ironcorebackend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    // Find schedules by class ID
    List<Schedule> findByClassEntity_Id(Long classId);
    
    // ⭐ NEW: Increment enrolled count when payment is completed
    @Modifying
    @Transactional
    @Query("UPDATE Schedule s SET s.enrolledCount = s.enrolledCount + 1 WHERE s.id = :scheduleId")
    void incrementEnrolledCount(@Param("scheduleId") Long scheduleId);
}