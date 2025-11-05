package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleResetService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleResetService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Automatically runs every day at midnight (00:00:00)
     * Resets past schedules to the next week and clears enrollment count
     */
    @Scheduled(cron = "0 0 0 * * ?") // Runs at midnight every day
    @Transactional
    public void resetPastSchedules() {
        LocalDate today = LocalDate.now();
        List<Schedule> allSchedules = scheduleRepository.findAll();

        int resetCount = 0;
        
        for (Schedule schedule : allSchedules) {
            // If schedule date is in the past
            if (schedule.getDate().isBefore(today)) {
                // Calculate next occurrence of this day
                LocalDate nextDate = getNextOccurrence(schedule.getDay(), today);
                
                // Reset enrollment count and update date
                schedule.setEnrolledCount(0);
                schedule.setDate(nextDate);
                scheduleRepository.save(schedule);
                
                resetCount++;
                System.out.println("Reset schedule: " + schedule.getDay() + " " + 
                                 schedule.getTimeSlot() + " from " + schedule.getDate() + 
                                 " to " + nextDate);
            }
        }
        
        if (resetCount > 0) {
            System.out.println("✅ Successfully reset " + resetCount + " past schedules");
        }
    }

    /**
     * Calculate the next occurrence of a specific day of the week
     */
    private LocalDate getNextOccurrence(String dayName, LocalDate fromDate) {
        DayOfWeek targetDay = parseDayOfWeek(dayName);
        DayOfWeek currentDay = fromDate.getDayOfWeek();

        int daysUntilNext = (targetDay.getValue() - currentDay.getValue() + 7) % 7;
        if (daysUntilNext == 0) daysUntilNext = 7; // always move to the next week

        return fromDate.plusDays(daysUntilNext);
    }

    /**
     * Convert day name string to DayOfWeek enum
     */
    private DayOfWeek parseDayOfWeek(String dayName) {
        switch (dayName.toUpperCase()) {
            case "MONDAY": return DayOfWeek.MONDAY;
            case "TUESDAY": return DayOfWeek.TUESDAY;
            case "WEDNESDAY": return DayOfWeek.WEDNESDAY;
            case "THURSDAY": return DayOfWeek.THURSDAY;
            case "FRIDAY": return DayOfWeek.FRIDAY;
            case "SATURDAY": return DayOfWeek.SATURDAY;
            case "SUNDAY": return DayOfWeek.SUNDAY;
            default: return DayOfWeek.MONDAY;
        }
    }

    /**
     * Manual reset endpoint - useful for testing
     */
    @Transactional
    public String manualReset() {
        resetPastSchedules();
        return "Schedule reset completed successfully";
    }
}