package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.repository.AppointmentRepository;
import com.tutorschedule.app.repository.StudentRepository;
import com.tutorschedule.app.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ScheduleAvailabilityService scheduleAvailabilityService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void getAppointments_returnsActiveUpcomingAppointments(){
        Appointment appointment = new Appointment(1L, 10L, 5L, LocalDate.now(), AppointmentStatus.ACTIVE);

        when(appointmentRepository.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(
                org.mockito.ArgumentMatchers.eq(AppointmentStatus.ACTIVE),
                org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(appointment));

        List<Appointment> result = appointmentService.getAppointments();

        assertEquals(List.of(appointment), result);
    }

    @Test
    void createAppointment_whenTeacherNotFound_throwsExceptionAndSkipsRemainingChecks(){
        when(teacherRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(1L, 10L, 5L, LocalDate.now())
        );

        assertEquals("Teacher not found: 1", exception.getMessage());
        verify(scheduleAvailabilityService, never()).isSlotAvailable(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAppointment_whenSlotNotAvailable_throwsException(){
        LocalDate date = LocalDate.now();
        Teacher teacher = mock(Teacher.class);

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));
        when(scheduleAvailabilityService.isSlotAvailable(1L, 10L, date))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(1L, 10L, 5L, date)
        );

        assertEquals("This slot is not available.", exception.getMessage());
    }

    @Test
    void createAppointment_whenStudentNotFound_throwsException(){
        LocalDate date = LocalDate.now();
        Teacher teacher = mock(Teacher.class);

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));
        when(scheduleAvailabilityService.isSlotAvailable(1L, 10L, date))
                .thenReturn(true);
        when(studentRepository.findById(5L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(1L, 10L, 5L, date)
        );

        assertEquals("Student not found: 5", exception.getMessage());
    }

    @Test
    void createAppointment_whenAllValid_savesAppointmentWithActiveStatus(){
        LocalDate date = LocalDate.now();
        Teacher teacher = mock(Teacher.class);
        Student student = mock(Student.class);

        when(teacherRepository.findById(1L))
                .thenReturn(Optional.of(teacher));
        when(scheduleAvailabilityService.isSlotAvailable(1L, 10L, date))
                .thenReturn(true);
        when(studentRepository.findById(5L))
                .thenReturn(Optional.of(student));
        when(appointmentRepository.save(org.mockito.ArgumentMatchers.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.createAppointment(1L, 10L, 5L, date);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment saved = captor.getValue();

        assertEquals(1L, saved.getTeacherId());
        assertEquals(10L, saved.getTimeSlotId());
        assertEquals(5L, saved.getStudentId());
        assertEquals(date, saved.getAppointmentDate());
        assertEquals(AppointmentStatus.ACTIVE, saved.getStatus());
        assertEquals(saved, result);
    }

    @Test
    void cancelAppointment_whenNotFound_throwsException(){
        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.cancelAppointment(1L)
        );

        assertEquals("Appointment not found: 1", exception.getMessage());
    }

    @Test
    void cancelAppointment_whenFound_setsStatusToCancelledAndSaves(){
        Appointment appointment = new Appointment(1L, 10L, 5L, LocalDate.now(), AppointmentStatus.ACTIVE);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        appointmentService.cancelAppointment(1L);

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }
}