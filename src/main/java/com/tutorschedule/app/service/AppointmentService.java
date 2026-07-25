package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import com.tutorschedule.app.repository.AppointmentRepository;
import com.tutorschedule.app.repository.StudentRepository;
import com.tutorschedule.app.repository.TeacherRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ScheduleAvailabilityService scheduleAvailabilityService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TeacherRepository teacherRepository,
                              StudentRepository studentRepository,
                              ScheduleAvailabilityService scheduleAvailabilityService){
        this.appointmentRepository = appointmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.scheduleAvailabilityService = scheduleAvailabilityService;
    }

    public List<Appointment> getAppointments(){
        return appointmentRepository.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(AppointmentStatus.ACTIVE,
                LocalDate.now());
    }

    @Transactional
    public Appointment createAppointment(Long teacherId, Long timeSlotId, Long studentId, LocalDate appointmentDate){
        if (teacherRepository.findById(teacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + teacherId);
        }

        boolean isAvailable = scheduleAvailabilityService.isSlotAvailable(teacherId, timeSlotId, appointmentDate);

        if (!isAvailable) {
            throw new IllegalArgumentException("This slot is not available.");
        }
        if (studentRepository.findById(studentId).isEmpty()) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }

        Appointment appointment = new Appointment(teacherId, timeSlotId, studentId, appointmentDate, AppointmentStatus.ACTIVE);

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void cancelAppointment(Long id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }
}
