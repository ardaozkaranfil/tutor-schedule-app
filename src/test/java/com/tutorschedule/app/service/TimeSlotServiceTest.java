package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimeSlotServiceTest {
    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherScheduleRepository teacherScheduleRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    @Test
    void getTimeSlotById_whenNotExists_throwsException(){
        when(timeSlotRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.getTimeSlotById(1L)
        );

        assertEquals("Zaman aralığı bulunamadı: 1", exception.getMessage());
    }

    @Test
    void deleteTimeSlot_deletesScheduleRowsAndTimeSlot(){
        TeacherSchedule scheduleRow = new TeacherSchedule(1L, 10L, null, java.time.DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);
        List<TeacherSchedule> rows = List.of(scheduleRow);

        when(teacherScheduleRepository.findByTimeSlotId(10L))
                .thenReturn(rows);

        timeSlotService.deleteTimeSlot(10L);

        verify(teacherScheduleRepository).deleteAll(rows);
        verify(timeSlotRepository).deleteById(10L);
    }

    @Test
    void addTimeSlot_whenEndTimeEqualsOrBeforeStartTime_throwsException(){
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14,0), LocalTime.of(13,0))
        );

        assertEquals("Bitiş saati başlangıç saatinden sonra olmalı", exception.getMessage());
    }

    @Test
    void addTimeSlot_whenNewSlotFullyOverlapsExisting_throwsException(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(13, 0), LocalTime.of(14, 0));

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(12, 30), LocalTime.of(14, 30))
        );
    }

    @Test
    void addTimeSlot_whenNewSlotFallsInsideExisting_throwsException(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(13, 0), LocalTime.of(14, 0));

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(13, 15), LocalTime.of(13, 45))
        );
    }

    @Test
    void addTimeSlot_whenNewSlotOnlyTouchesEdge_doesNotThrow(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(13, 0), LocalTime.of(14, 0));

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(existing));
        when(timeSlotRepository.save(any(TimeSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRepository.findAll())
                .thenReturn(List.of());

        TimeSlot result = timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));

        assertEquals(LocalTime.of(14, 0), result.getStartTime());
    }
    @Test
    void addTimeSlot_savesWithCorrectFields(){
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of());
        when(timeSlotRepository.save(any(TimeSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14,0), LocalTime.of(15,0));

        ArgumentCaptor<TimeSlot> captor = ArgumentCaptor.forClass(TimeSlot.class);

        verify(timeSlotRepository).save(captor.capture());

        TimeSlot capturedSlot = captor.getValue();

        assertEquals(TimeSlotDayType.WEEKDAY, capturedSlot.getDayType());
        assertEquals(LocalTime.of(14, 0), capturedSlot.getStartTime());
        assertEquals(LocalTime.of(15, 0), capturedSlot.getEndTime());
    }

    @Test
    void updateTimeSlot_whenNotExists_throwsException(){
        when(timeSlotRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.updateTimeSlot(1L, LocalTime.of(14, 0), LocalTime.of(14, 40))
        );
    }

    @Test
    void updateTimeSlot_whenEndTimeNotAfterStartTime_throwsException(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(10, 0), LocalTime.of(10, 40));
        existing.setId(1L);

        when(timeSlotRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.updateTimeSlot(1L, LocalTime.of(14, 40), LocalTime.of(14, 0))
        );

        assertEquals("Bitiş saati başlangıç saatinden sonra olmalı", exception.getMessage());
    }

    @Test
    void updateTimeSlot_whenOverlapsAnotherSlot_throwsException(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(10, 0), LocalTime.of(10, 40));
        existing.setId(1L);

        TimeSlot otherSlot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 20), LocalTime.of(15, 0));
        otherSlot.setId(2L);

        when(timeSlotRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(existing, otherSlot));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> timeSlotService.updateTimeSlot(1L, LocalTime.of(14, 0), LocalTime.of(14, 40))
        );

        assertEquals("Bu saat mevcut bir saatle çakışıyor: 14:20 - 15:00", exception.getMessage());
    }

    @Test
    void updateTimeSlot_whenValid_updatesAndSaves(){
        TimeSlot existing = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(10, 0), LocalTime.of(10, 40));
        existing.setId(1L);

        when(timeSlotRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(existing));
        when(timeSlotRepository.save(existing))
                .thenReturn(existing);

        TimeSlot result = timeSlotService.updateTimeSlot(1L, LocalTime.of(14, 0), LocalTime.of(14, 40));

        assertEquals(LocalTime.of(14, 0), result.getStartTime());
        assertEquals(LocalTime.of(14, 40), result.getEndTime());
        verify(timeSlotRepository).save(existing);
    }
}
