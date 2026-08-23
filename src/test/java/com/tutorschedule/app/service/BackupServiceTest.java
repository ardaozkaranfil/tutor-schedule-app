package com.tutorschedule.app.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BackupServiceTest {

    private final BackupService backupService = new BackupService();

    @Test
    void restore_whenBackupFileDoesNotExist_throwsException(){
        File missingFile = new File("nonexistent_" + UUID.randomUUID() + ".sql");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> backupService.restore(missingFile)
        );

        assertEquals("Yedek dosyası bulunamadı: " + missingFile.getName(), exception.getMessage());
    }
}