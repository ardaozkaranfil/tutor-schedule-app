package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.*;
import com.tutorschedule.app.repository.AppointmentRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleAvailabilityServiceTest {

    @Mock
    private TeacherScheduleRepository teacherScheduleRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private ScheduleAvailabilityService scheduleAvailabilityService;

    @Test
    void getTeacherDayAvailability_whenNoTemplateEntry_returnsBlocked(){
        Teacher teacher = mock(Teacher.class);
        when(teacher.getId()).thenReturn(1L);
        LocalDate date = LocalDate.of(2024, 1, 1);

        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, null, null);
        slot.setId(10L);

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(slot));
        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of());
        when(appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date))
                .thenReturn(List.of());

        Map<TimeSlot, TeacherScheduleStatus> result = scheduleAvailabilityService.getTeacherDayAvailability(teacher, date);

        assertEquals(TeacherScheduleStatus.BLOCKED, result.get(slot));
    }

    @Test
    void getTeacherDayAvailability_whenFreeTemplateHasActiveAppointment_returnsBusy(){
        Teacher teacher = mock(Teacher.class);
        when(teacher.getId()).thenReturn(1L);
        LocalDate date = LocalDate.of(2024, 1, 1);

        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, null, null);
        slot.setId(10L);

        TeacherSchedule templateEntry = new TeacherSchedule(1L, 10L, null, date.getDayOfWeek(), TeacherScheduleStatus.FREE);
        Appointment activeAppointment = new Appointment(1L, 10L, 5L, date, AppointmentStatus.ACTIVE);

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(slot));
        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of(templateEntry));
        when(appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date))
                .thenReturn(List.of(activeAppointment));

        Map<TimeSlot, TeacherScheduleStatus> result = scheduleAvailabilityService.getTeacherDayAvailability(teacher, date);

        assertEquals(TeacherScheduleStatus.BUSY, result.get(slot));
    }

    @Test
    void getTeacherDayAvailability_whenFreeTemplateNoAppointment_returnsFreeAndUsesWeekendSlots(){
        Teacher teacher = mock(Teacher.class);
        when(teacher.getId()).thenReturn(1L);
        LocalDate date = LocalDate.of(2024, 1, 6); // Saturday

        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKEND, null, null);
        slot.setId(20L);

        TeacherSchedule templateEntry = new TeacherSchedule(1L, 20L, null, date.getDayOfWeek(), TeacherScheduleStatus.FREE);

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND))
                .thenReturn(List.of(slot));
        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of(templateEntry));
        when(appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date))
                .thenReturn(List.of());

        Map<TimeSlot, TeacherScheduleStatus> result = scheduleAvailabilityService.getTeacherDayAvailability(teacher, date);

        assertEquals(TeacherScheduleStatus.FREE, result.get(slot));
        verify(timeSlotRepository).findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND);
    }

    @Test
    void isSlotAvailable_whenTemplateNotFree_returnsFalse(){
        LocalDate date = LocalDate.of(2024, 1, 1);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of());

        boolean result = scheduleAvailabilityService.isSlotAvailable(1L, 10L, date);

        assertFalse(result);
    }

    @Test
    void isSlotAvailable_whenFreeButActiveAppointmentExists_returnsFalse(){
        LocalDate date = LocalDate.of(2024, 1, 1);

        TeacherSchedule templateEntry = new TeacherSchedule(1L, 10L, null, date.getDayOfWeek(), TeacherScheduleStatus.FREE);
        Appointment activeAppointment = new Appointment(1L, 10L, 5L, date, AppointmentStatus.ACTIVE);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of(templateEntry));
        when(appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date))
                .thenReturn(List.of(activeAppointment));

        boolean result = scheduleAvailabilityService.isSlotAvailable(1L, 10L, date);

        assertFalse(result);
    }

    @Test
    void isSlotAvailable_whenFreeAndNoActiveAppointment_returnsTrue(){
        LocalDate date = LocalDate.of(2024, 1, 1);

        TeacherSchedule templateEntry = new TeacherSchedule(1L, 10L, null, date.getDayOfWeek(), TeacherScheduleStatus.FREE);
        Appointment cancelledAppointment = new Appointment(1L, 10L, 5L, date, AppointmentStatus.CANCELLED);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, date.getDayOfWeek()))
                .thenReturn(List.of(templateEntry));
        when(appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date))
                .thenReturn(List.of(cancelledAppointment));

        boolean result = scheduleAvailabilityService.isSlotAvailable(1L, 10L, date);

        assertTrue(result);
    }
}