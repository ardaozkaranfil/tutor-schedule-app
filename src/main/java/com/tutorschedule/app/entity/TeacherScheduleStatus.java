package com.tutorschedule.app.entity;

/**
 * A teacher's schedule state for a given day/slot. FREE: bookable.
 * BUSY: locked to a fixed class. BLOCKED: teacher doesn't work that hour.
 */
public enum TeacherScheduleStatus {
    BUSY, FREE, BLOCKED
}
