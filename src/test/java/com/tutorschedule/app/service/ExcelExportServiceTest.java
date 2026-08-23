package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExcelExportServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private ExcelExportService excelExportService;

    @Test
    void exportTeacherSchedule_whenTeacherNotFound_throwsException(){
        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> excelExportService.exportTeacherSchedule(1L)
        );
    }

    @Test
    void exportTeacherSchedule_buildsHeaderAndMarksStatusesCorrectly() throws IOException {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(mock(Teacher.class)));

        TimeSlot weekdaySlot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(9, 0), LocalTime.of(9, 40));
        weekdaySlot.setId(10L);

        Map<DayOfWeek, List<TeacherSchedule>> weeklySchedule = new LinkedHashMap<>();
        weeklySchedule.put(DayOfWeek.MONDAY, List.of(
                new TeacherSchedule(1L, 10L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE)));
        weeklySchedule.put(DayOfWeek.TUESDAY, List.of(
                new TeacherSchedule(1L, 10L, "12-MF", DayOfWeek.TUESDAY, TeacherScheduleStatus.BUSY)));
        weeklySchedule.put(DayOfWeek.WEDNESDAY, List.of(
                new TeacherSchedule(1L, 10L, null, DayOfWeek.WEDNESDAY, TeacherScheduleStatus.BLOCKED)));

        when(scheduleService.getWeeklySchedule(1L)).thenReturn(weeklySchedule);
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(weekdaySlot));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND))
                .thenReturn(List.of());

        byte[] bytes = excelExportService.exportTeacherSchedule(1L);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertEquals("Saat", header.getCell(0).getStringCellValue());
            assertEquals("Pazartesi", header.getCell(1).getStringCellValue());
            assertEquals("Pazar", header.getCell(7).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals("09:00-09:40", dataRow.getCell(0).getStringCellValue());
            assertEquals("Boş", dataRow.getCell(1).getStringCellValue());       // Pazartesi
            assertEquals("12-MF", dataRow.getCell(2).getStringCellValue());     // Salı
            assertEquals("Bloklu", dataRow.getCell(3).getStringCellValue());   // Çarşamba
            assertEquals("-", dataRow.getCell(4).getStringCellValue());         // Thursday, no entry
            assertEquals("-", dataRow.getCell(6).getStringCellValue());         // Saturday, no weekend slots
        }
    }

    @Test
    void exportTeacherSchedule_fillsShorterSideWithDashWhenSlotCountsDiffer() throws IOException {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(mock(Teacher.class)));

        TimeSlot weekdaySlotA = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(9, 0), LocalTime.of(9, 40));
        weekdaySlotA.setId(10L);
        TimeSlot weekdaySlotB = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(10, 0), LocalTime.of(10, 40));
        weekdaySlotB.setId(11L);
        TimeSlot weekendSlot = new TimeSlot(TimeSlotDayType.WEEKEND, LocalTime.of(11, 0), LocalTime.of(11, 40));
        weekendSlot.setId(20L);

        when(scheduleService.getWeeklySchedule(1L)).thenReturn(Map.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY))
                .thenReturn(List.of(weekdaySlotA, weekdaySlotB));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND))
                .thenReturn(List.of(weekendSlot));

        byte[] bytes = excelExportService.exportTeacherSchedule(1L);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);

            // 2 weekday slots vs 1 weekend slot -> rowCount = 2
            Row secondDataRow = sheet.getRow(2); // rowIdx = 1
            assertEquals("10:00-10:40", secondDataRow.getCell(0).getStringCellValue());
            assertEquals("-", secondDataRow.getCell(6).getStringCellValue()); // Saturday, weekend slots exhausted
            assertEquals("-", secondDataRow.getCell(7).getStringCellValue()); // Sunday, weekend slots exhausted
        }
    }
}