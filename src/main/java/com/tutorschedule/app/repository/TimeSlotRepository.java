package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Database access layer for the TimeSlot entity.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Returns weekday or weekend time slots ordered by start time.
     */
    List<TimeSlot> findByDayTypeOrderByStartTimeAsc(TimeSlotDayType dayType);
}
