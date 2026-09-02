package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.AppointmentService;
import com.tutorschedule.app.service.ScheduleAvailabilityService;
import com.tutorschedule.app.service.StudentService;
import com.tutorschedule.app.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private ScheduleAvailabilityService scheduleAvailabilityService;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private TimeSlotRepository timeSlotRepository;

    @Test
    void showAppointments_weekdayDate_returnsListView() throws Exception {
        LocalDate monday = LocalDate.of(2026, 8, 31);
        Teacher teacher = new Teacher("Arda", "Matematik");

        when(teacherService.getAllTeachers()).thenReturn(List.of(teacher));
        when(teacherService.getDistinctBranches()).thenReturn(List.of("Matematik"));
        when(scheduleAvailabilityService.getTeacherDayAvailability(teacher, monday)).thenReturn(Map.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(appointmentService.getAppointments()).thenReturn(List.of());

        mockMvc.perform(get("/appointments").param("date", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/list"))
                .andExpect(model().attribute("selectedDate", monday))
                .andExpect(model().attributeExists("branchAvailability"))
                .andExpect(model().attributeExists("upcomingAppointments"));
    }

    @Test
    void showAppointments_withTeacherId_narrowsRowsToOneTeacher() throws Exception {
        LocalDate monday = LocalDate.of(2026, 8, 31);
        Teacher teacher = new Teacher("Arda", "Matematik");

        when(teacherService.getAllTeachers()).thenReturn(List.of(teacher));
        when(teacherService.getTeacherById(1L)).thenReturn(teacher);
        when(teacherService.getDistinctBranches()).thenReturn(List.of());
        when(scheduleAvailabilityService.getTeacherDayAvailability(teacher, monday)).thenReturn(Map.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(appointmentService.getAppointments()).thenReturn(List.of());

        mockMvc.perform(get("/appointments").param("date", "2026-08-31").param("teacherId", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedTeacherId", 1L));

        verify(teacherService).getTeacherById(1L);
    }

    @Test
    void showAppointments_withUpcomingAppointment_resolvesNamesAndTime() throws Exception {
        LocalDate monday = LocalDate.of(2026, 8, 31);
        Teacher teacher = new Teacher("Arda", "Matematik");
        Student student = new Student();
        student.setId(5L);
        student.setFullName("Ayşe");
        student.setClassName("12-MF");
        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));
        Appointment appointment = new Appointment(10L, 20L, 5L, monday, AppointmentStatus.ACTIVE);

        when(teacherService.getAllTeachers()).thenReturn(List.of(teacher));
        when(teacherService.getDistinctBranches()).thenReturn(List.of());
        when(scheduleAvailabilityService.getTeacherDayAvailability(teacher, monday)).thenReturn(Map.of());
        when(timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY)).thenReturn(List.of());
        when(appointmentService.getAppointments()).thenReturn(List.of(appointment));
        when(teacherService.findTeacherById(10L)).thenReturn(Optional.of(teacher));
        when(studentService.findStudentById(5L)).thenReturn(Optional.of(student));
        when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(slot));

        mockMvc.perform(get("/appointments").param("date", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("upcomingAppointments"));
    }

    @Test
    void bookAppointment_validInput_redirectsWithDateAndBranch() throws Exception {
        mockMvc.perform(post("/appointments/book")
                        .param("teacherId", "1")
                        .param("timeSlotId", "2")
                        .param("studentId", "3")
                        .param("appointmentDate", "2026-08-31")
                        .param("branch", "Matematik"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments?date=2026-08-31&branch=Matematik"));

        verify(appointmentService).createAppointment(1L, 2L, 3L, LocalDate.of(2026, 8, 31));
    }

    @Test
    void bookAppointment_slotUnavailable_setsFlashErrorAndRedirects() throws Exception {
        when(appointmentService.createAppointment(1L, 2L, 3L, LocalDate.of(2026, 8, 31)))
                .thenThrow(new IllegalArgumentException("Bu saat müsait değil."));

        mockMvc.perform(post("/appointments/book")
                        .param("teacherId", "1")
                        .param("timeSlotId", "2")
                        .param("studentId", "3")
                        .param("appointmentDate", "2026-08-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments?date=2026-08-31"))
                .andExpect(flash().attribute("errorMessage", "Bu saat müsait değil."));
    }

    @Test
    void cancelAppointment_redirectsToAppointmentsList() throws Exception {
        mockMvc.perform(post("/appointments/cancel/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"));

        verify(appointmentService).cancelAppointment(1L);
    }

    @Test
    void history_withStudentId_returnsSummaryAndTeacherBranchPerRow() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 20);
        Teacher teacher = new Teacher("Arda", "Matematik");
        Student student = new Student();
        student.setId(5L);
        student.setFullName("Ayşe");
        student.setClassName("12-MF");
        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));

        Appointment active = new Appointment(10L, 20L, 5L, date, AppointmentStatus.ACTIVE);
        Appointment cancelled = new Appointment(10L, 20L, 5L, date.minusDays(3), AppointmentStatus.CANCELLED);

        when(appointmentService.getStudentHistory(5L)).thenReturn(List.of(active, cancelled));
        when(teacherService.findTeacherById(10L)).thenReturn(Optional.of(teacher));
        when(studentService.findStudentById(5L)).thenReturn(Optional.of(student));
        when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(slot));

        mockMvc.perform(get("/appointments/history").param("studentId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.cancelled").value(1))
                .andExpect(jsonPath("$.rows[0].date").value("2026-08-20"))
                .andExpect(jsonPath("$.rows[0].time").value("14:00 - 14:40"))
                .andExpect(jsonPath("$.rows[0].teacherName").value("Arda"))
                .andExpect(jsonPath("$.rows[0].branch").value("Matematik"))
                .andExpect(jsonPath("$.rows[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.rows[1].status").value("CANCELLED"));

        verify(appointmentService).getStudentHistory(5L);
    }

    @Test
    void history_withTeacherId_returnsStudentAndClassPerRow() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 20);
        Teacher teacher = new Teacher("Arda", "Matematik");
        Student student = new Student();
        student.setId(5L);
        student.setFullName("Ayşe");
        student.setClassName("12-MF");
        TimeSlot slot = new TimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));

        Appointment appointment = new Appointment(10L, 20L, 5L, date, AppointmentStatus.ACTIVE);

        when(appointmentService.getTeacherHistory(10L)).thenReturn(List.of(appointment));
        when(teacherService.findTeacherById(10L)).thenReturn(Optional.of(teacher));
        when(studentService.findStudentById(5L)).thenReturn(Optional.of(student));
        when(timeSlotRepository.findById(20L)).thenReturn(Optional.of(slot));

        mockMvc.perform(get("/appointments/history").param("teacherId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.cancelled").value(0))
                .andExpect(jsonPath("$.rows[0].studentName").value("Ayşe"))
                .andExpect(jsonPath("$.rows[0].className").value("12-MF"));

        verify(appointmentService).getTeacherHistory(10L);
    }

    @Test
    void history_withNoParams_returnsEmptyViewWithoutTouchingService() throws Exception {
        mockMvc.perform(get("/appointments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.cancelled").value(0))
                .andExpect(jsonPath("$.rows").isEmpty());

        verify(appointmentService, never()).getStudentHistory(anyLong());
        verify(appointmentService, never()).getTeacherHistory(anyLong());
    }

    @Test
    void history_deletedTeacherOrMissingSlot_fallsBackToPlaceholders() throws Exception {
        Appointment appointment = new Appointment(10L, 20L, 5L, LocalDate.of(2026, 8, 20), AppointmentStatus.ACTIVE);

        when(appointmentService.getStudentHistory(5L)).thenReturn(List.of(appointment));
        when(teacherService.findTeacherById(10L)).thenReturn(Optional.empty());
        when(studentService.findStudentById(5L)).thenReturn(Optional.of(new Student()));
        when(timeSlotRepository.findById(20L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/appointments/history").param("studentId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].teacherName").value("(silinmiş öğretmen)"))
                .andExpect(jsonPath("$.rows[0].branch").value("—"))
                .andExpect(jsonPath("$.rows[0].time").value(""));
    }
}