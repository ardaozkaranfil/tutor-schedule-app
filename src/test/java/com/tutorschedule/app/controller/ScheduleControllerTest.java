package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.ExcelExportService;
import com.tutorschedule.app.service.ScheduleService;
import com.tutorschedule.app.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private ExcelExportService excelExportService;

    @MockitoBean
    private TimeSlotRepository timeSlotRepository;

    @Test
    void showSchedule_noTeachers_returnsGridViewWithoutSelectedTeacher() throws Exception {
        when(teacherService.getAllTeachers()).thenReturn(List.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND)).thenReturn(List.of());

        mockMvc.perform(get("/schedule"))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule/grid"))
                .andExpect(model().attribute("selectedTeacher", nullValue()))
                .andExpect(model().attributeDoesNotExist("scheduleGrid"));
    }

    @Test
    void showSchedule_withTeacherId_buildsScheduleGrid() throws Exception {
        Teacher teacher = new Teacher("Arda", "Matematik");
        TeacherSchedule entry = new TeacherSchedule(1L, 2L, null, DayOfWeek.MONDAY, TeacherScheduleStatus.FREE);

        when(teacherService.getAllTeachers()).thenReturn(List.of(teacher));
        when(teacherService.getTeacherById(1L)).thenReturn(teacher);
        when(scheduleService.getWeeklySchedule(1L)).thenReturn(Map.of(DayOfWeek.MONDAY, List.of(entry)));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND)).thenReturn(List.of());

        mockMvc.perform(get("/schedule").param("teacherId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("schedule/grid"))
                .andExpect(model().attribute("selectedTeacher", teacher))
                .andExpect(model().attributeExists("scheduleGrid"));
    }

    @Test
    void updateCell_validInput_redirectsWithTeacherId() throws Exception {
        mockMvc.perform(post("/schedule/update")
                        .param("teacherId", "1")
                        .param("dayOfWeek", "MONDAY")
                        .param("timeSlotId", "2")
                        .param("status", "BUSY")
                        .param("className", "12-MF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedule?teacherId=1"));

        verify(scheduleService).updateScheduleEntry(1L, DayOfWeek.MONDAY, 2L, TeacherScheduleStatus.BUSY, "12-MF");
    }

    @Test
    void updateCell_invalidClass_setsFlashErrorAndRedirects() throws Exception {
        when(scheduleService.updateScheduleEntry(1L, DayOfWeek.MONDAY, 2L, TeacherScheduleStatus.BUSY, "99-Z"))
                .thenThrow(new IllegalArgumentException("Sınıf bulunamadı: 99-Z"));

        mockMvc.perform(post("/schedule/update")
                        .param("teacherId", "1")
                        .param("dayOfWeek", "MONDAY")
                        .param("timeSlotId", "2")
                        .param("status", "BUSY")
                        .param("className", "99-Z"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/schedule?teacherId=1"))
                .andExpect(flash().attribute("errorMessage", "Sınıf bulunamadı: 99-Z"));
    }

    @Test
    void exportSchedule_returnsExcelFileWithContentDisposition() throws Exception {
        LocalDate weekReference = LocalDate.of(2026, 8, 31);
        byte[] excelBytes = {1, 2, 3};

        when(excelExportService.exportTeacherSchedule(1L, weekReference)).thenReturn(excelBytes);
        when(excelExportService.buildExportFileBaseName(1L, weekReference)).thenReturn("Arda_2026-08-31_haftasi");
        when(excelExportService.toAsciiFallback("Arda_2026-08-31_haftasi.xlsx")).thenReturn("Arda_2026-08-31_haftasi.xlsx");

        mockMvc.perform(get("/schedule/export/{teacherId}", 1L).param("date", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(excelBytes))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("Arda_2026-08-31_haftasi.xlsx")));
    }
}