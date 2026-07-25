package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByTeacherIdAndTimeSlotIdAndAppointmentDate(Long teacherId, Long timeSlotId, LocalDate appointmentDate);
    List<Appointment> findByTeacherIdAndAppointmentDate(Long teacherId, LocalDate appointmentDate);
    List<Appointment> findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(
            AppointmentStatus status, LocalDate date);
}
