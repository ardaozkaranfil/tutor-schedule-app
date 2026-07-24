package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeSlotService {

    private static final List<DayOfWeek> SCHOOL_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    private final TimeSlotRepository timeSlotRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherScheduleRepository teacherScheduleRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository,
                           TeacherRepository teacherRepository,
                           TeacherScheduleRepository teacherScheduleRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.teacherRepository = teacherRepository;
        this.teacherScheduleRepository = teacherScheduleRepository;
    }

    public List<TimeSlot> getWeekdayTimeSlots() {
        return timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY);
    }

    public List<TimeSlot> getWeekendTimeSlots() {
        return timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND);
    }

    @Transactional
    public TimeSlot addTimeSlot(TimeSlotDayType dayType, LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        List<TimeSlot> existing = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType);
        for (TimeSlot slot : existing) {
            boolean overlaps = startTime.isBefore(slot.getEndTime()) && slot.getStartTime().isBefore(endTime);
            if (overlaps) {
                throw new IllegalArgumentException("Time slot overlaps with an existing one: "
                        + slot.getStartTime() + " - " + slot.getEndTime());
            }
        }

        TimeSlot saved = timeSlotRepository.save(new TimeSlot(dayType, startTime, endTime));

        List<TeacherSchedule> toSave = new ArrayList<>();
        List<Teacher> teachers = teacherRepository.findAll();
        for (DayOfWeek day : SCHOOL_DAYS) {
            TimeSlotDayType daySlotType = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                    ? TimeSlotDayType.WEEKEND
                    : TimeSlotDayType.WEEKDAY;
            if (daySlotType != dayType) {
                continue;
            }
            for (Teacher teacher : teachers) {
                toSave.add(new TeacherSchedule(teacher.getId(), saved.getId(), null, day, TeacherScheduleStatus.FREE));
            }
        }
        teacherScheduleRepository.saveAll(toSave);

        return saved;
    }
}