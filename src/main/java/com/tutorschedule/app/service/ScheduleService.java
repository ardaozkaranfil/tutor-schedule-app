package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.repository.ClassGroupRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final TeacherScheduleRepository teacherScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ClassGroupRepository classGroupRepository;

    public ScheduleService(TeacherScheduleRepository teacherScheduleRepository,
                           TimeSlotRepository timeSlotRepository,
                           ClassGroupRepository classGroupRepository) {
        this.teacherScheduleRepository = teacherScheduleRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.classGroupRepository = classGroupRepository;
    }

    public Map<DayOfWeek, List<TeacherSchedule>> getWeeklySchedule(Long teacherId) {
        List<TeacherSchedule> entries = teacherScheduleRepository.findByTeacherId(teacherId);

        Map<Long, TimeSlot> slotsById = timeSlotRepository.findAll().stream()
                .collect(Collectors.toMap(TimeSlot::getId, slot -> slot));

        Map<DayOfWeek, List<TeacherSchedule>> byDay = new EnumMap<>(DayOfWeek.class);
        for (TeacherSchedule entry : entries) {
            byDay.computeIfAbsent(entry.getDayOfWeek(), d -> new ArrayList<>()).add(entry);
        }
        for (List<TeacherSchedule> dayEntries : byDay.values()) {
            dayEntries.sort(Comparator.comparing(e -> slotsById.get(e.getTimeSlotId()).getStartTime()));
        }
        return byDay;
    }

    @Transactional
    public TeacherSchedule updateScheduleEntry(Long teacherId, DayOfWeek dayOfWeek, Long timeSlotId,
                                               TeacherScheduleStatus status, String className) {
        TeacherSchedule entry = teacherScheduleRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek).stream()
                .filter(e -> e.getTimeSlotId().equals(timeSlotId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No schedule entry for teacher " + teacherId + " on " + dayOfWeek + " at slot " + timeSlotId));

        if (status == TeacherScheduleStatus.BUSY) {
            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("Class name is required when marking a slot as busy");
            }
            if (!classGroupRepository.existsById(className)) {
                throw new IllegalArgumentException("Class not found: " + className);
            }
            entry.setClassName(className);
        } else {
            entry.setClassName(null);
        }

        entry.setStatus(status);
        return teacherScheduleRepository.save(entry);
    }
}
