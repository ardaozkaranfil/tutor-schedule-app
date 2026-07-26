package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.BackupTrigger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupBackupRunner implements ApplicationRunner {

    private final BackupService backupService;

    public StartupBackupRunner(BackupService backupService) {
        this.backupService = backupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        backupService.performBackup(BackupTrigger.STARTUP);
    }
}