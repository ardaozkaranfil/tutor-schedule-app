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

/**
 * Backs up the database via mysqldump and can restore from a backup file
 * when needed. Backups run @Async so they don't hold up the caller (e.g.
 * saving a schedule). Files are written to the backups/ folder, named with
 * a timestamp plus the trigger label (see BackupTrigger) — there's no
 * separate Backup entity or table, listings are read straight off the
 * filesystem.
 */
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

    /**
     * Runs mysqldump and writes the output as a .sql file into backups/.
     * If mysqldump exits with a non-zero code, the half-written file is
     * deleted and the failure is logged; no exception is thrown here since
     * this method runs asynchronously and the caller has no way to catch it.
     */
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

    /**
     * Restores the database from the given .sql backup file via the mysql
     * command. If the file doesn't exist or mysql exits with an error code,
     * the operation is aborted with an exception — this one is deliberately
     * synchronous, we don't want the app carrying on in a half-restored state.
     */
    public void restore(File backupFile) {
        if (!backupFile.exists()) {
            throw new IllegalArgumentException("Yedek dosyası bulunamadı: " + backupFile.getName());
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
                throw new IllegalStateException("Geri yükleme başarısız oldu, çıkış kodu: " + exitCode);
            }

            log.info("Restore completed from: {}", backupFile.getName());
        } catch (java.io.IOException | InterruptedException e) {
            throw new IllegalStateException("Dosya için geri yükleme başarısız: " + backupFile.getName(), e);
        }
    }
}