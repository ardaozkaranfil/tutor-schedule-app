package com.tutorschedule.app.entity;

/**
 * The state of an appointment. CANCELLED appointments are never deleted
 * from the database, they're just excluded from listings and availability
 * checks.
 */
public enum AppointmentStatus {
    ACTIVE, CANCELLED
}
