package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private TimeSlotRepository timeSlotRepository;

    @Test
    void listTeachers_noParams_returnsTeacherListView() throws Exception {
        when(teacherService.searchTeachers(null, null)).thenReturn(List.of());
        when(teacherService.getDistinctBranches()).thenReturn(List.of("Matematik"));

        mockMvc.perform(get("/teachers"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/list"))
                .andExpect(model().attributeExists("teachers"))
                .andExpect(model().attributeExists("branches"));
    }

    @Test
    void listTeachers_withNameAndBranch_passesBothToService() throws Exception {
        when(teacherService.searchTeachers("Arda", "Matematik")).thenReturn(List.of());
        when(teacherService.getDistinctBranches()).thenReturn(List.of());

        mockMvc.perform(get("/teachers").param("name", "Arda").param("branch", "Matematik"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedBranch", "Matematik"));

        verify(teacherService).searchTeachers("Arda", "Matematik");
    }

    @Test
    void searchTeachers_returnsJsonList() throws Exception {
        Teacher teacher = new Teacher("Arda", "Matematik");
        when(teacherService.searchTeachers("Arda", null)).thenReturn(List.of(teacher));

        mockMvc.perform(get("/teachers/search").param("name", "Arda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Arda"))
                .andExpect(jsonPath("$[0].branch").value("Matematik"));
    }

    @Test
    void deleteTeacher_redirectsToTeachersList() throws Exception {
        mockMvc.perform(post("/teachers/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers"));

        verify(teacherService).deleteTeacher(1L);
    }

    @Test
    void showAddForm_returnsFormViewWithWeeklyGrid() throws Exception {
        when(teacherService.getDistinctBranches()).thenReturn(List.of("Matematik"));
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND)).thenReturn(List.of());

        mockMvc.perform(get("/teachers/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/form"))
                .andExpect(model().attributeExists("teacher"))
                .andExpect(model().attributeExists("branches"))
                .andExpect(model().attributeExists("weekdaySlots"))
                .andExpect(model().attributeExists("weekendSlots"));
    }

    @Test
    void showEditForm_returnsFormViewWithExistingTeacher() throws Exception {
        Teacher teacher = new Teacher("Arda", "Matematik");
        when(teacherService.getTeacherById(1L)).thenReturn(teacher);
        when(teacherService.getDistinctBranches()).thenReturn(List.of("Matematik"));

        mockMvc.perform(get("/teachers/edit/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/form"))
                .andExpect(model().attribute("teacher", teacher));
    }

    @Test
    void submitAddForm_noScheduleCells_createsTeacherWithEmptySchedule() throws Exception {
        mockMvc.perform(post("/teachers/add")
                        .param("fullName", "Arda")
                        .param("branch", "Matematik"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers"));

        verify(teacherService).createTeacher(eq("Arda"), eq("Matematik"), eq(List.of()));
    }

    @Test
    void submitEditForm_redirectsToTeachersList() throws Exception {
        mockMvc.perform(post("/teachers/edit/{id}", 1L)
                        .param("fullName", "Arda Yeni")
                        .param("branch", "Fizik"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teachers"));

        verify(teacherService).updateTeacher(1L, "Arda Yeni", "Fizik");
    }
}