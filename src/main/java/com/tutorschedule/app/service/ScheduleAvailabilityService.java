package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.AppointmentRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleAvailabilityService {

    private final TeacherScheduleRepository teacherScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;

    public ScheduleAvailabilityService(TeacherScheduleRepository teacherScheduleRepository,
                                       TimeSlotRepository timeSlotRepository,
                                       AppointmentRepository appointmentRepository) {
        this.teacherScheduleRepository = teacherScheduleRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public Map<TimeSlot, TeacherScheduleStatus> getTeacherDayAvailability(Teacher teacher, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        TimeSlotDayType dayType = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
                ? TimeSlotDayType.WEEKEND
                : TimeSlotDayType.WEEKDAY;

        List<TimeSlot> daySlots = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType);
        List<TeacherSchedule> weeklyEntries = teacherScheduleRepository.findByTeacherIdAndDayOfWeek(teacher.getId(), dayOfWeek);
        List<Appointment> dayAppointments = appointmentRepository.findByTeacherIdAndAppointmentDate(teacher.getId(), date);

        Map<TimeSlot, TeacherScheduleStatus> result = new LinkedHashMap<>();
        for (TimeSlot slot : daySlots) {
            TeacherScheduleStatus templateStatus = weeklyEntries.stream()
                    .filter(e -> e.getTimeSlotId().equals(slot.getId()))
                    .findFirst()
                    .map(TeacherSchedule::getStatus)
                    .orElse(TeacherScheduleStatus.BLOCKED);

            boolean hasActiveAppointment = dayAppointments.stream()
                    .anyMatch(a -> a.getTimeSlotId().equals(slot.getId()) && a.getStatus() == AppointmentStatus.ACTIVE);

            TeacherScheduleStatus effectiveStatus = templateStatus;
            if (templateStatus == TeacherScheduleStatus.FREE && hasActiveAppointment) {
                effectiveStatus = TeacherScheduleStatus.BUSY;
            }

            result.put(slot, effectiveStatus);
        }
        return result;
    }

    public boolean isSlotAvailable(Long teacherId, Long timeSlotId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        TeacherScheduleStatus templateStatus = teacherScheduleRepository.findByTeacherIdAndDayOfWeek(teacherId, dayOfWeek).stream()
                .filter(e -> e.getTimeSlotId().equals(timeSlotId))
                .findFirst()
                .map(TeacherSchedule::getStatus)
                .orElse(TeacherScheduleStatus.BLOCKED);

        if (templateStatus != TeacherScheduleStatus.FREE) {
            return false;
        }

        boolean hasActiveAppointment = appointmentRepository.findByTeacherIdAndAppointmentDate(teacherId, date).stream()
                .anyMatch(a -> a.getTimeSlotId().equals(timeSlotId) && a.getStatus() == AppointmentStatus.ACTIVE);

        return !hasActiveAppointment;
    }
}