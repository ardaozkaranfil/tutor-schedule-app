package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.BackupTrigger;
import com.tutorschedule.app.service.BackupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles the "Yedekleme" screen: listing existing .sql backups (read
 * straight off the backups/ folder — there's no Backup entity/table) and
 * restoring the database from a selected one. Taking a backup itself isn't
 * triggered here; that happens automatically via {@link BackupService} on
 * startup / schedule save.
 */
@Controller
@RequestMapping("/backups")
public class BackupController {

    private static final Path BACKUP_DIR = Path.of("backups");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Lists backup files newest-first for the "Yedekleme" page, parsing
     * each file name (yyyy-MM-dd_HHmm_trigger.sql) into date/time/trigger.
     * Files that don't match the expected naming pattern are skipped
     * instead of blowing up the whole page.
     */
    @GetMapping
    public String listBackups(Model model) {
        model.addAttribute("backups", readBackupEntries());
        return "backup/list";
    }

    /**
     * Restores the database from the given backup file. The file name is
     * validated before it's resolved against BACKUP_DIR to rule out path
     * traversal, even though in practice it only ever comes from the list
     * this controller itself renders.
     */
    @PostMapping("/restore/{fileName}")
    public String restore(@PathVariable String fileName, RedirectAttributes redirectAttributes) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Geçersiz dosya adı");
            return "redirect:/backups";
        }

        try {
            File backupFile = BACKUP_DIR.resolve(fileName).toFile();
            backupService.restore(backupFile);
            redirectAttributes.addFlashAttribute("successMessage", "Yedek başarıyla geri yüklendi: " + fileName);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/backups";
    }

    /**
     * Reads backups/ and parses each .sql file name into a {@link BackupEntry},
     * sorted newest-first. Returns an empty list if the folder doesn't exist
     * yet (e.g. no backup has ever been taken).
     */
    private List<BackupEntry> readBackupEntries() {
        List<BackupEntry> entries = new ArrayList<>();
        if (!Files.exists(BACKUP_DIR)) {
            return entries;
        }

        try (Stream<Path> files = Files.list(BACKUP_DIR)) {
            files.filter(path -> path.toString().endsWith(".sql"))
                    .forEach(path -> parseBackupEntry(path.getFileName().toString()).ifPresent(entries::add));
        } catch (IOException e) {
            throw new IllegalStateException("Backup klasörü okunamadı", e);
        }

        entries.sort(Comparator.comparing(BackupEntry::fileName).reversed());
        return entries;
    }

    /**
     * Parses a single file name like "2026-07-16_0902_startup.sql" into its
     * date, time and trigger label. Returns empty instead of throwing if the
     * name doesn't follow the expected format, so one bad/manual file in the
     * folder doesn't break the whole listing.
     */
    private java.util.Optional<BackupEntry> parseBackupEntry(String fileName) {
        String base = fileName.substring(0, fileName.length() - ".sql".length());
        String[] parts = base.split("_", 3);
        if (parts.length != 3) {
            return java.util.Optional.empty();
        }

        try {
            LocalDate date = LocalDate.parse(parts[0], DATE_FORMAT);
            LocalTime time = LocalTime.parse(parts[1], TIME_FORMAT);
            String triggerLabel = triggerDisplayName(parts[2]);
            return java.util.Optional.of(new BackupEntry(fileName, date, time, triggerLabel));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Maps a trigger's file-name label (see {@link BackupTrigger#getLabel()})
     * back to the Turkish text shown on the "Yedekleme" page. Falls back to
     * the raw label for any value that doesn't match a known trigger.
     */
    private String triggerDisplayName(String label) {
        for (BackupTrigger trigger : BackupTrigger.values()) {
            if (trigger.getLabel().equals(label)) {
                return switch (trigger) {
                    case STARTUP -> "Uygulama açılışı";
                    case SCHEDULE_SAVE -> "Ders programı kaydı";
                };
            }
        }
        return label;
    }

    /**
     * Row shown on the "Yedekleme" page: one parsed backup file.
     */
    public record BackupEntry(String fileName, LocalDate date, LocalTime time, String triggerLabel) {
    }
}