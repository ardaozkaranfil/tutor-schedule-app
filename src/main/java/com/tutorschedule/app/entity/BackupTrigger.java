package com.tutorschedule.app.entity;

public enum BackupTrigger {
    STARTUP("startup"),
    SCHEDULE_SAVE("schedule-save");

    private final String label;

    BackupTrigger(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}