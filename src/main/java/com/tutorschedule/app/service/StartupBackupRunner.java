package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.BackupTrigger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Kicks off an automatic backup as soon as the app starts. Runs once via
 * Spring Boot's ApplicationRunner mechanism, right after the context is
 * fully up.
 */
@Component
public class StartupBackupRunner implements ApplicationRunner {

    private final BackupService backupService;

    public StartupBackupRunner(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Triggers a backup labeled STARTUP when the application boots.
     */
    @Override
    public void run(ApplicationArguments args) {
        backupService.performBackup(BackupTrigger.STARTUP);
    }
}