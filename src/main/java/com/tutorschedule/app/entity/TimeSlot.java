package com.tutorschedule.app.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "TIME_SLOT")
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlotDayType dayType;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    public TimeSlot(){

    }
    public TimeSlot(TimeSlotDayType dayType, LocalTime startTime, LocalTime endTime){
        this.dayType = dayType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public TimeSlotDayType getDayType() {return dayType;}
    public void setDayType(TimeSlotDayType dayType) {this.dayType = dayType;}
    public LocalTime getStartTime() {return startTime;}
    public void setStartTime(LocalTime startTime) {this.startTime = startTime;}
    public LocalTime getEndTime() {return endTime;}
    public void setEndTime(LocalTime endTime) {this.endTime = endTime;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeSlot timeSlot = (TimeSlot) o;
        return id != null && id.equals(timeSlot.getId());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
