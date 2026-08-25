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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void exportTeacherSchedule_buildsTwoTablesAndMarksStatusesCorrectly() throws IOException {
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

            // Row 0: section titles
            Row titleRow = sheet.getRow(0);
            assertEquals("Haftaiçi Programı", titleRow.getCell(0).getStringCellValue());
            assertEquals("Haftasonu Programı", titleRow.getCell(7).getStringCellValue());

            // Row 1: "Saat" + day-name headers for both tables
            Row header = sheet.getRow(1);
            assertEquals("Saat", header.getCell(0).getStringCellValue());
            assertEquals("Pazartesi", header.getCell(1).getStringCellValue());
            assertEquals("Cuma", header.getCell(5).getStringCellValue());
            assertEquals("Saat", header.getCell(7).getStringCellValue());
            assertEquals("Cumartesi", header.getCell(8).getStringCellValue());
            assertEquals("Pazar", header.getCell(9).getStringCellValue());

            // Row 2: first (and only) weekday data row
            Row dataRow = sheet.getRow(2);
            assertEquals("09:00-09:40", dataRow.getCell(0).getStringCellValue());
            assertEquals("Boş", dataRow.getCell(1).getStringCellValue());       // Pazartesi
            assertEquals("12-MF", dataRow.getCell(2).getStringCellValue());     // Salı
            assertEquals("Bloklu", dataRow.getCell(3).getStringCellValue());   // Çarşamba
            assertEquals("-", dataRow.getCell(4).getStringCellValue());         // Perşembe, no entry

            // No weekend slots at all -> weekend table has no data cells in this row
            assertNull(dataRow.getCell(7));
            assertNull(dataRow.getCell(8));
            assertNull(dataRow.getCell(9));
        }
    }

    @Test
    void exportTeacherSchedule_weekdayAndWeekendTablesHaveIndependentRowCounts() throws IOException {
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

            // Row 2 (first data row): both tables have a row here
            Row firstDataRow = sheet.getRow(2);
            assertEquals("09:00-09:40", firstDataRow.getCell(0).getStringCellValue());
            assertEquals("-", firstDataRow.getCell(1).getStringCellValue());
            assertEquals("11:00-11:40", firstDataRow.getCell(7).getStringCellValue());
            assertEquals("-", firstDataRow.getCell(8).getStringCellValue());
            assertEquals("-", firstDataRow.getCell(9).getStringCellValue());

            // Row 3 (second data row): only the weekday table has a second slot,
            // the weekend table is exhausted so its columns stay untouched.
            Row secondDataRow = sheet.getRow(3);
            assertEquals("10:00-10:40", secondDataRow.getCell(0).getStringCellValue());
            assertNull(secondDataRow.getCell(7));
            assertNull(secondDataRow.getCell(8));
            assertNull(secondDataRow.getCell(9));
        }
    }
}