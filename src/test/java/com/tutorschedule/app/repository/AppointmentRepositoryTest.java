package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Appointment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static com.tutorschedule.app.entity.AppointmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void findByTeacherIdAndAppointmentDate_returnsOnlyThatTeacherAndDate() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, date, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 2L, 11L, date.plusDays(1), ACTIVE));
        appointmentRepository.save(new Appointment(2L, 1L, 12L, date, ACTIVE));

        List<Appointment> result = appointmentRepository.findByTeacherIdAndAppointmentDate(1L, date);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStudentId()).isEqualTo(10L);
    }

    @Test
    void findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc_excludesPastDates() {
        LocalDate today = LocalDate.of(2026, 8, 30);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, today.minusDays(1), ACTIVE));
        appointmentRepository.save(new Appointment(1L, 2L, 11L, today, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 3L, 12L, today.plusDays(1), ACTIVE));

        List<Appointment> result = appointmentRepository.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(ACTIVE, today);

        assertThat(result).hasSize(2)
                .extracting(Appointment::getStudentId)
                .containsExactly(11L, 12L);
    }

    @Test
    void findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc_excludesOtherStatus() {
        LocalDate today = LocalDate.of(2026, 8, 30);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, today, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 2L, 11L, today, CANCELLED));

        List<Appointment> result = appointmentRepository.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(ACTIVE, today);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStudentId()).isEqualTo(10L);
    }

    @Test
    void findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc_ordersByDateAsc() {
        LocalDate today = LocalDate.of(2026, 8, 30);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, today.plusDays(5), ACTIVE));
        appointmentRepository.save(new Appointment(1L, 2L, 11L, today, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 3L, 12L, today.plusDays(2), ACTIVE));

        List<Appointment> result = appointmentRepository.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAsc(ACTIVE, today);

        assertThat(result)
                .extracting(Appointment::getAppointmentDate)
                .containsExactly(today, today.plusDays(2), today.plusDays(5));
    }

    @Test
    void findByTeacherIdAndStatusAndAppointmentDateBetween_filtersInclusiveRange() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 7);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, start.minusDays(1), ACTIVE));
        appointmentRepository.save(new Appointment(1L, 2L, 11L, start, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 3L, 12L, end, ACTIVE));
        appointmentRepository.save(new Appointment(1L, 4L, 13L, end.plusDays(1), ACTIVE));
        appointmentRepository.save(new Appointment(1L, 5L, 14L, start.plusDays(2), CANCELLED));
        appointmentRepository.save(new Appointment(2L, 6L, 15L, start.plusDays(2), ACTIVE));

        List<Appointment> result = appointmentRepository.findByTeacherIdAndStatusAndAppointmentDateBetween(1L, ACTIVE, start, end);

        assertThat(result).hasSize(2)
                .extracting(Appointment::getStudentId)
                .containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    void findByStudentIdOrderByAppointmentDateDesc_returnsOnlyThatStudentNewestFirstIncludingCancelled() {
        LocalDate base = LocalDate.of(2026, 9, 1);
        appointmentRepository.save(new Appointment(1L, 1L, 10L, base, ACTIVE));
        appointmentRepository.save(new Appointment(2L, 2L, 10L, base.plusDays(10), CANCELLED));
        appointmentRepository.save(new Appointment(3L, 3L, 10L, base.plusDays(5), ACTIVE));
        appointmentRepository.save(new Appointment(1L, 1L, 99L, base.plusDays(3), ACTIVE));

        List<Appointment> result = appointmentRepository.findByStudentIdOrderByAppointmentDateDesc(10L);

        assertThat(result).hasSize(3)
                .extracting(Appointment::getAppointmentDate)
                .containsExactly(base.plusDays(10), base.plusDays(5), base);
        assertThat(result).extracting(Appointment::getStatus).contains(CANCELLED);
    }

    @Test
    void findByTeacherIdOrderByAppointmentDateDesc_returnsOnlyThatTeacherNewestFirstIncludingCancelled() {
        LocalDate base = LocalDate.of(2026, 9, 1);
        appointmentRepository.save(new Appointment(7L, 1L, 10L, base, ACTIVE));
        appointmentRepository.save(new Appointment(7L, 2L, 11L, base.plusDays(8), CANCELLED));
        appointmentRepository.save(new Appointment(7L, 3L, 12L, base.plusDays(2), ACTIVE));
        appointmentRepository.save(new Appointment(8L, 1L, 13L, base.plusDays(4), ACTIVE));

        List<Appointment> result = appointmentRepository.findByTeacherIdOrderByAppointmentDateDesc(7L);

        assertThat(result).hasSize(3)
                .extracting(Appointment::getAppointmentDate)
                .containsExactly(base.plusDays(8), base.plusDays(2), base);
        assertThat(result).extracting(Appointment::getStatus).contains(CANCELLED);
    }
}