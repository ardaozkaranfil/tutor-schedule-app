package com.tutorschedule.app.entity;

import jakarta.persistence.*;

import java.time.DayOfWeek;

@Entity
@Table(name = "TEACHER_SCHEDULE")
public class TeacherSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private Long timeSlotId;

    @Column(nullable = true)
    private String className;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherScheduleStatus status;

    public TeacherSchedule(){

    }
    public TeacherSchedule(Long teacherId, Long timeSlotId, String className, DayOfWeek dayOfWeek, TeacherScheduleStatus status){
        this.teacherId = teacherId;
        this.timeSlotId =timeSlotId;
        this.className = className;
        this.dayOfWeek = dayOfWeek;
        this.status = status;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Long getTeacherId() {return teacherId;}
    public void setTeacherId(Long teacherId) {this.teacherId = teacherId;}
    public Long getTimeSlotId() {return timeSlotId;}
    public void setTimeSlotId(Long timeSlotId) {this.timeSlotId = timeSlotId;}
    public String getClassName() {return className;}
    public void setClassName(String className) {this.className = className;}
    public DayOfWeek getDayOfWeek() {return dayOfWeek;}
    public void setDayOfWeek(DayOfWeek dayOfWeek) {this.dayOfWeek = dayOfWeek;}
    public TeacherScheduleStatus getStatus() {return status;}
    public void setStatus(TeacherScheduleStatus status) {this.status = status;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TeacherSchedule teacherSchedule = (TeacherSchedule) o;
        return id != null && id.equals(teacherSchedule.getId());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
