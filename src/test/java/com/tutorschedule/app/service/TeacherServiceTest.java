package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeacherScheduleRepository teacherScheduleRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void getTeacherById_whenNotExists_throwsException(){
        when(teacherRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.getTeacherById(1L)
        );

        assertEquals("Öğretmen bulunamadı: 1", exception.getMessage());
    }

    @Test
    void searchTeachers_whenNameAndBranchProvided_callsCombinedQuery(){
        String name = "Arda";
        String branch = "Math";

        when(teacherRepository.findByFullNameContainingIgnoreCaseAndBranch(name, branch))
                .thenReturn(List.of());

        teacherService.searchTeachers(name, branch);

        verify(teacherRepository).findByFullNameContainingIgnoreCaseAndBranch(name, branch);
        verify(teacherRepository, never()).findAll();
    }

    @Test
    void searchTeachers_whenOnlyNameProvided_callsFindByName(){
        String name = "Arda";

        when(teacherRepository.findByFullNameContainingIgnoreCase(name))
                .thenReturn(List.of());

        teacherService.searchTeachers(name, null);

        verify(teacherRepository).findByFullNameContainingIgnoreCase(name);
        verify(teacherRepository, never()).findAll();
    }

    @Test
    void searchTeachers_whenOnlyBranchProvided_callsFindByBranch(){
        String branch = "Math";

        when(teacherRepository.findByBranch(branch))
                .thenReturn(List.of());

        teacherService.searchTeachers(null, branch);

        verify(teacherRepository).findByBranch(branch);
        verify(teacherRepository, never()).findAll();
    }

    @Test
    void searchTeachers_whenNeitherProvided_callsFindAll(){
        when(teacherRepository.findAll())
                .thenReturn(List.of());

        teacherService.searchTeachers(null, null);

        verify(teacherRepository).findAll();
    }

    @Test
    void createTeacher_savesTeacherAndBuildsWeeklyScheduleAndTakesBackup(){
        Teacher savedTeacher = mock(Teacher.class);
        when(savedTeacher.getId()).thenReturn(1L);
        when(teacherRepository.save(any(Teacher.class)))
                .thenReturn(savedTeacher);

        TimeSlot weekdaySlot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));
        weekdaySlot.setId(10L);
        TimeSlot weekendSlot = new TimeSlot(TimeSlotDayType.WEEKEND, LocalTime.of(10, 0), LocalTime.of(10, 40));
        weekendSlot.setId(20L);

        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(weekdaySlot));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND))
                .thenReturn(List.of(weekendSlot));

        Teacher result = teacherService.createTeacher("Arda", "Math", List.of());

        assertEquals(savedTeacher, result);

        ArgumentCaptor<List<TeacherSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(teacherScheduleRepository).saveAll(captor.capture());
        List<TeacherSchedule> savedSchedules = captor.getValue();
        assertEquals(7, savedSchedules.size());

        verify(backupService).performBackup(BackupTrigger.SCHEDULE_SAVE);
    }

    @Test
    void updateTeacher_whenNotExists_throwsException(){
        when(teacherRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.updateTeacher(1L, "Arda", "Math")
        );
    }

    @Test
    void updateTeacher_whenExists_updatesAndSaves(){
        Teacher existing = new Teacher("Old Name", "Old Branch");

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(teacherRepository.save(existing))
                .thenReturn(existing);

        Teacher result = teacherService.updateTeacher(1L, "New Name", "New Branch");

        assertEquals("New Name", result.getFullName());
        assertEquals("New Branch", result.getBranch());
        verify(teacherRepository).save(existing);
    }

    @Test
    void deleteTeacher_deletesScheduleRowsAndTeacher(){
        TeacherSchedule scheduleRow = new TeacherSchedule(1L, 10L, null, java.time.DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);
        List<TeacherSchedule> rows = List.of(scheduleRow);

        when(teacherScheduleRepository.findByTeacherId(1L))
                .thenReturn(rows);

        teacherService.deleteTeacher(1L);

        verify(teacherScheduleRepository).deleteAll(rows);
        verify(teacherRepository).deleteById(1L);
    }

    @Test
    void findTeacherById_whenExists_returnsTeacher() {
        Teacher teacher = new Teacher();
        teacher.setFullName("Zeynep");
        teacher.setBranch("Matematik");
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        Optional<Teacher> result = teacherService.findTeacherById(1L);

        assertTrue(result.isPresent());
        assertEquals("Zeynep", result.get().getFullName());
    }

    @Test
    void findTeacherById_whenNotExists_returnsEmptyWithoutThrowing() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertTrue(teacherService.findTeacherById(1L).isEmpty());
    }
}