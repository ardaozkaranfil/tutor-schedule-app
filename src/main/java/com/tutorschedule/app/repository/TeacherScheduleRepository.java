package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.TeacherSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Database access layer for the TeacherSchedule entity.
 */
@Repository
public interface TeacherScheduleRepository extends JpaRepository<TeacherSchedule, Long> {

    /**
     * Returns all of a teacher's weekly schedule rows (7 days x every slot).
     */
    List<TeacherSchedule> findByTeacherId(Long teacherId);

    /**
     * Returns a teacher's schedule rows for one specific day of the week.
     */
    List<TeacherSchedule> findByTeacherIdAndDayOfWeek(Long teacherId, DayOfWeek dayOfWeek);
}
