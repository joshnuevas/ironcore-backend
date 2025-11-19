package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Enrolled;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrolledRepository extends JpaRepository<Enrolled, Long> {
    boolean existsByUserIdAndClassEntityId(Long userId, Long classId);
}
