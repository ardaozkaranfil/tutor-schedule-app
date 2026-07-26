package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Database access layer for the Appointment entity.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Checks whether an appointment already exists for the given teacher,
     * time slot, and date, regardless of its status.
     */
    boolean existsByTeacherIdAndTimeSlotIdAndAppointmentDate(Long teacherId, Long timeSlotId, LocalDate appointmentDate);

    /**
     * Returns all of a teacher's appointments on a given date; used by
     * availability calculations (ScheduleAvailabilityService).
     */
    List<Appointment> findByTeacherIdAndAppointmentDate(Long teacherId, LocalDate appointmentDate);

    /**
     * Returns appointments with the given status from the given date onward
     * (past ones excluded), ordered by date ascending. Used on the appointments
     * screen to keep past and cancelled entries out of the list.
     */
    List<Appointment> findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(
            AppointmentStatus status, LocalDate date);
}
