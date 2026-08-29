package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.TeacherSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.DayOfWeek;
import java.util.List;

import static com.tutorschedule.app.entity.TeacherScheduleStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeacherScheduleRepositoryTest {

    @Autowired
    private TeacherScheduleRepository teacherScheduleRepository;

    @Test
    void findByTeacherId_returnsAllRowsForTeacher() {
        teacherScheduleRepository.save(new TeacherSchedule(1L, 1L, null, DayOfWeek.MONDAY, FREE));
        teacherScheduleRepository.save(new TeacherSchedule(1L, 2L, null, DayOfWeek.TUESDAY, BLOCKED));
        teacherScheduleRepository.save(new TeacherSchedule(2L, 1L, null, DayOfWeek.MONDAY, FREE));

        List<TeacherSchedule> result = teacherScheduleRepository.findByTeacherId(1L);

        assertThat(result).hasSize(2)
                .extracting(TeacherSchedule::getTeacherId)
                .containsOnly(1L);
    }

    @Test
    void findByTeacherIdAndDayOfWeek_filtersToOneDay() {
        teacherScheduleRepository.save(new TeacherSchedule(1L, 1L, null, DayOfWeek.MONDAY, FREE));
        teacherScheduleRepository.save(new TeacherSchedule(1L, 2L, null, DayOfWeek.TUESDAY, BLOCKED));
        teacherScheduleRepository.save(new TeacherSchedule(1L, 3L, null, DayOfWeek.MONDAY, BUSY));

        List<TeacherSchedule> result = teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY);

        assertThat(result).hasSize(2)
                .extracting(TeacherSchedule::getDayOfWeek)
                .containsOnly(DayOfWeek.MONDAY);
    }

    @Test
    void findByTimeSlotId_returnsRowsUsingThatSlot() {
        teacherScheduleRepository.save(new TeacherSchedule(1L, 5L, null, DayOfWeek.MONDAY, FREE));
        teacherScheduleRepository.save(new TeacherSchedule(2L, 5L, null, DayOfWeek.TUESDAY, BUSY));
        teacherScheduleRepository.save(new TeacherSchedule(3L, 6L, null, DayOfWeek.MONDAY, BLOCKED));

        List<TeacherSchedule> result = teacherScheduleRepository.findByTimeSlotId(5L);

        assertThat(result).hasSize(2)
                .extracting(TeacherSchedule::getTimeSlotId)
                .containsOnly(5L);
    }
}