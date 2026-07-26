package com.tutorschedule.app.entity;

/**
 * Marks what event caused a backup to be taken. Gets written into the
 * backup file name (via the `label` field), so the reason for a given
 * backup can be read straight off the file in the backups folder.
 */
public enum BackupTrigger {
    STARTUP("startup"),
    SCHEDULE_SAVE("schedule-save");

    private final String label;

    BackupTrigger(String label) {
        this.label = label;
    }

    /**
     * Short label used in the backup file name (e.g. "startup", "schedule-save").
     */
    public String getLabel() {
        return label;
    }
}