package com.ironcore.ironcorebackend.repository;

import java.util.List;
import com.ironcore.ironcorebackend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByClassEntity_Id(Long classId);
}
