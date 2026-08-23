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

/**
 * Manages time slot definitions (weekday/weekend hour ranges). Adding a
 * new slot opens up a FREE schedule row for every teacher, for every day
 * that matches the slot's day type.
 */
@Service
public class TimeSlotService {

    /**
     * The seven days walked over when a new slot triggers a teacher-schedule
     * update.
     */
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

    /**
     * Returns weekday time slots ordered by start time.
     */
    public List<TimeSlot> getWeekdayTimeSlots() {
        return timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY);
    }

    /**
     * Returns weekend time slots ordered by start time.
     */
    public List<TimeSlot> getWeekendTimeSlots() {
        return timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND);
    }

    /**
     * Defines a new time slot. End time must be after start time, otherwise
     * IllegalArgumentException is thrown. The new slot is also rejected if it
     * overlaps an existing slot of the same day type. Once saved, a FREE
     * schedule row is opened for every teacher, for every day matching the
     * slot's day type.
     */
    @Transactional
    public TimeSlot addTimeSlot(TimeSlotDayType dayType, LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Bitiş saati başlangıç saatinden sonra olmalı");
        }

        List<TimeSlot> existing = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType);
        for (TimeSlot slot : existing) {
            boolean overlaps = startTime.isBefore(slot.getEndTime()) && slot.getStartTime().isBefore(endTime);
            if (overlaps) {
                throw new IllegalArgumentException("Bu saat mevcut bir saatle çakışıyor: "
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

    /**
     * Fetches the time slot with the given id; throws IllegalArgumentException
     * if none exists.
     */
    public TimeSlot getTimeSlotById(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zaman aralığı bulunamadı: " + id));
    }

    /**
     * Updates an existing time slot's start/end time. dayType is not
     * editable here — a slot can't jump between the weekday and weekend
     * lists once created. Same validation as addTimeSlot: end time must be
     * after start time, and the new range must not overlap any other slot
     * of the same day type.
     */
    @Transactional
    public TimeSlot updateTimeSlot(Long id, LocalTime startTime, LocalTime endTime) {
        TimeSlot existing = getTimeSlotById(id);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Bitiş saati başlangıç saatinden sonra olmalı");
        }

        List<TimeSlot> siblings = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(existing.getDayType());
        for (TimeSlot slot : siblings) {
            if (slot.getId().equals(id)) {
                continue;
            }
            boolean overlaps = startTime.isBefore(slot.getEndTime()) && slot.getStartTime().isBefore(endTime);
            if (overlaps) {
                throw new IllegalArgumentException("Bu saat mevcut bir saatle çakışıyor: "
                        + slot.getStartTime() + " - " + slot.getEndTime());
            }
        }

        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        return timeSlotRepository.save(existing);
    }

    /**
     * Deletes a time slot along with every teacher-schedule row that
     * referenced it (one per teacher per day matching its day type).
     */
    @Transactional
    public void deleteTimeSlot(Long id) {
        teacherScheduleRepository.deleteAll(teacherScheduleRepository.findByTimeSlotId(id));
        timeSlotRepository.deleteById(id);
    }
}