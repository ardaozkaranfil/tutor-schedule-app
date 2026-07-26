package com.tutorschedule.app.entity;

/**
 * Marks whether a time slot belongs to the weekday or weekend definition.
 * Saturday and Sunday count as WEEKEND, every other day is WEEKDAY.
 */
public enum TimeSlotDayType {
    WEEKEND, WEEKDAY
}
