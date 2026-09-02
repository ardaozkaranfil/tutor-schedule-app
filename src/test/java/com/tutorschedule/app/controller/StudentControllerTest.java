package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.ClassGroup;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.ClassGroupRepository;
import com.tutorschedule.app.service.ExcelImportService;
import com.tutorschedule.app.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private ExcelImportService excelImportService;

    @MockitoBean
    private ClassGroupRepository classGroupRepository;

    @Test
    void listStudents_noNameParam_returnsStudentListView() throws Exception {
        when(studentService.searchStudents(null)).thenReturn(List.of());
        when(classGroupRepository.findAll()).thenReturn(List.of(new ClassGroup("12-MF")));

        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/list"))
                .andExpect(model().attributeExists("students"))
                .andExpect(model().attributeExists("classGroups"));
    }

    @Test
    void listStudents_withNameParam_passesNameToService() throws Exception {
        when(studentService.searchStudents("Arda")).thenReturn(List.of());
        when(classGroupRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/students").param("name", "Arda"))
                .andExpect(status().isOk());

        verify(studentService).searchStudents("Arda");
    }

    @Test
    void searchStudent_returnsJsonList() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Arda");
        student.setClassName("12-MF");

        when(studentService.searchStudents("Arda")).thenReturn(List.of(student));

        mockMvc.perform(get("/students/search").param("name", "Arda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Arda"));
    }

    @Test
    void submitAddForm_validInput_redirectsToStudentsList() throws Exception {
        mockMvc.perform(post("/students/add")
                        .param("fullName", "Arda")
                        .param("className", "12-MF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"));

        verify(studentService).createStudent("12-MF", "Arda");
    }

    @Test
    void importFromExcel_success_setsImportSummaryFlash() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        when(excelImportService.importFromExcel(any())).thenReturn("3 öğrenci eklendi");

        mockMvc.perform(multipart("/students/import").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attribute("importSummary", "3 öğrenci eklendi"));
    }

    @Test
    void importFromExcel_failure_setsErrorMessageFlash() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        when(excelImportService.importFromExcel(any()))
                .thenThrow(new RuntimeException("Dosya okunamadı"));

        mockMvc.perform(multipart("/students/import").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"))
                .andExpect(flash().attribute("errorMessage", "Dosya okunamadı"));
    }

    @Test
    void deleteStudent_redirectsToStudentsList() throws Exception {
        mockMvc.perform(post("/students/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"));

        verify(studentService).deleteStudent(1L);
    }

    @Test
    void editStudent_redirectsToStudentsList() throws Exception {
        mockMvc.perform(post("/students/edit/{id}", 1L)
                        .param("fullName", "Arda Yeni")
                        .param("className", "12-MF"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/students"));

        verify(studentService).updateStudent(1L, "12-MF", "Arda Yeni");
    }

    @Test
    void getStudent_returnsStudentJson() throws Exception {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Arda");
        student.setClassName("12-MF");

        when(studentService.getStudentById(1L)).thenReturn(student);

        mockMvc.perform(get("/students/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Arda"));
    }
}