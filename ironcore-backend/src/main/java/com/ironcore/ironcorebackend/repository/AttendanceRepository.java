package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Find attendance by user and date
    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    // Find all attendance records for a specific user
    List<Attendance> findByUserIdOrderByAttendanceDateDesc(Long userId);

    // Find all attendance records for a specific date
    List<Attendance> findByAttendanceDateOrderByUserUsername(LocalDate attendanceDate);
    
    // Find all attendance records for a specific date (simpler version)
    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    // Find attendance records within a date range
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate BETWEEN :startDate AND :endDate ORDER BY a.attendanceDate DESC, a.user.username")
    List<Attendance> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Find checked-in users for a specific date
    List<Attendance> findByAttendanceDateAndCheckedInTrue(LocalDate attendanceDate);

    // Count attendance for a user within a date range
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.attendanceDate BETWEEN :startDate AND :endDate AND a.checkedIn = true")
    Long countUserAttendance(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}