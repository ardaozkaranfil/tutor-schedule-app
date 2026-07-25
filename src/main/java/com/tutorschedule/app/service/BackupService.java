package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.BackupTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final Path BACKUP_DIR = Path.of("backups");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.db-name}")
    private String dbName;

    @Async
    public void performBackup(BackupTrigger trigger) {
        try {
            Files.createDirectories(BACKUP_DIR);

            String fileName = LocalDateTime.now().format(FILE_NAME_FORMAT) + "_" + trigger.getLabel() + ".sql";
            File outputFile = BACKUP_DIR.resolve(fileName).toFile();

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "mysqldump", "-u", dbUser, dbName
            );
            processBuilder.environment().put("MYSQL_PWD", dbPassword);
            processBuilder.redirectOutput(outputFile);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Backup failed with exit code {} for trigger {}", exitCode, trigger);
                Files.deleteIfExists(outputFile.toPath());
                return;
            }

            log.info("Backup created: {}", fileName);
        } catch (Exception e) {
            log.error("Backup failed for trigger {}", trigger, e);
        }
    }

    public void restore(File backupFile) {
        if (!backupFile.exists()) {
            throw new IllegalArgumentException("Backup file not found: " + backupFile.getName());
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "mysql", "-u", dbUser, dbName
            );
            processBuilder.environment().put("MYSQL_PWD", dbPassword);
            processBuilder.redirectInput(backupFile);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Restore failed with exit code " + exitCode);
            }

            log.info("Restore completed from: {}", backupFile.getName());
        } catch (java.io.IOException | InterruptedException e) {
            throw new IllegalStateException("Restore failed for file " + backupFile.getName(), e);
        }
    }
}