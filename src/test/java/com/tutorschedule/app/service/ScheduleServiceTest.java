package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.BackupTrigger;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.ClassGroupRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private TeacherScheduleRepository teacherScheduleRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void getWeeklySchedule_groupsEntriesByDay(){
        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(9, 0), LocalTime.of(9, 40));
        slot.setId(10L);

        TeacherSchedule mondayEntry = new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);
        TeacherSchedule tuesdayEntry = new TeacherSchedule(1L, 10L, null, DayOfWeek.TUESDAY, TeacherScheduleStatus.FREE);

        when(teacherScheduleRepository.findByTeacherId(1L))
                .thenReturn(List.of(mondayEntry, tuesdayEntry));
        when(timeSlotRepository.findAll())
                .thenReturn(List.of(slot));

        Map<DayOfWeek, List<TeacherSchedule>> result = scheduleService.getWeeklySchedule(1L);

        assertEquals(List.of(mondayEntry), result.get(DayOfWeek.MONDAY));
        assertEquals(List.of(tuesdayEntry), result.get(DayOfWeek.TUESDAY));
    }

    @Test
    void getWeeklySchedule_sortsSameDayEntriesByStartTime(){
        TimeSlot laterSlot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));
        laterSlot.setId(20L);
        TimeSlot earlierSlot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(9, 0), LocalTime.of(9, 40));
        earlierSlot.setId(10L);

        TeacherSchedule laterEntry = new TeacherSchedule(1L, 20L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);
        TeacherSchedule earlierEntry = new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);

        when(teacherScheduleRepository.findByTeacherId(1L))
                .thenReturn(List.of(laterEntry, earlierEntry));
        when(timeSlotRepository.findAll())
                .thenReturn(List.of(laterSlot, earlierSlot));

        Map<DayOfWeek, List<TeacherSchedule>> result = scheduleService.getWeeklySchedule(1L);

        assertEquals(List.of(earlierEntry, laterEntry), result.get(DayOfWeek.MONDAY));
    }

    @Test
    void updateScheduleEntry_whenNoMatchingEntry_throwsException(){
        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 10L, TeacherScheduleStatus.FREE, null)
        );
    }

    @Test
    void updateScheduleEntry_whenBusyWithoutClassName_throwsException(){
        TeacherSchedule entry = new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(entry));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 10L, TeacherScheduleStatus.BUSY, "  ")
        );

        assertEquals("Bir saati meşgul olarak işaretlemek için sınıf adı zorunludur", exception.getMessage());
    }

    @Test
    void updateScheduleEntry_whenBusyWithUnknownClassName_throwsException(){
        TeacherSchedule entry = new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(entry));
        when(classGroupRepository.existsById("12-MF"))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 10L, TeacherScheduleStatus.BUSY, "12-MF")
        );

        assertEquals("Sınıf bulunamadı: 12-MF", exception.getMessage());
    }

    @Test
    void updateScheduleEntry_whenBusyWithValidClassName_updatesAndBacksUp(){
        TeacherSchedule entry = new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(entry));
        when(classGroupRepository.existsById("12-MF"))
                .thenReturn(true);
        when(teacherScheduleRepository.save(entry))
                .thenReturn(entry);

        TeacherSchedule result = scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 10L, TeacherScheduleStatus.BUSY, "12-MF");

        assertEquals("12-MF", result.getClassName());
        assertEquals(TeacherScheduleStatus.BUSY, result.getStatus());
        verify(teacherScheduleRepository).save(entry);
        verify(backupService).performBackup(BackupTrigger.SCHEDULE_SAVE);
    }

    @Test
    void updateScheduleEntry_whenNotBusy_clearsClassName(){
        TeacherSchedule entry = new TeacherSchedule(1L, 10L, "12-MF", DayOfWeek.MONDAY, TeacherScheduleStatus.BUSY);

        when(teacherScheduleRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(entry));
        when(teacherScheduleRepository.save(entry))
                .thenReturn(entry);

        TeacherSchedule result = scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 10L, TeacherScheduleStatus.FREE, "12-MF");

        assertNull(result.getClassName());
        assertEquals(TeacherScheduleStatus.FREE, result.getStatus());
        verify(classGroupRepository, never()).existsById(org.mockito.ArgumentMatchers.any());
    }
}