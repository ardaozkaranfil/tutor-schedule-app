package com.tutorschedule.app.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "APPOINTMENT")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private Long timeSlotId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    public Appointment(){

    }
    public Appointment(Long teacherId, Long timeSlotId, Long studentId, LocalDate appointmentDate, AppointmentStatus status){
        this.teacherId = teacherId;
        this.timeSlotId = timeSlotId;
        this.studentId = studentId;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }


    public Long getId() {return id;}
    public Long getTeacherId() {return teacherId;}
    public void setTeacherId(Long teacherId) {this.teacherId = teacherId;}
    public Long getTimeSlotId() {return timeSlotId;}
    public void setTimeSlotId(Long timeSlotId) {this.timeSlotId = timeSlotId;}
    public Long getStudentId() {return studentId;}
    public void setStudentId(Long studentId) {this.studentId = studentId;}
    public LocalDate getAppointmentDate() {return appointmentDate;}
    public void setAppointmentDate(LocalDate appointmentDate) {this.appointmentDate = appointmentDate;}
    public AppointmentStatus getStatus() {return status;}
    public void setStatus(AppointmentStatus status) {this.status = status;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Appointment appointment = (Appointment) o;
        return Objects.equals(id, appointment.getId());
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }
}