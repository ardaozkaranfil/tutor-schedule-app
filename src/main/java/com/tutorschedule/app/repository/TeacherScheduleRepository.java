package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.TeacherSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface TeacherScheduleRepository extends JpaRepository<TeacherSchedule, Long> {
    List<TeacherSchedule> findByTeacherId(Long teacherId);
    List<TeacherSchedule> findByTeacherIdAndDayOfWeek(Long teacherId, DayOfWeek dayOfWeek);
}
