package com.tutorschedule.app.controller;

import com.tutorschedule.app.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BackupController.class)
class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackupService backupService;

    @Test
    void listBackups_returnsParsedEntryFromBackupsFolder() throws Exception {
        Path backupDir = Path.of("backups");
        Files.createDirectories(backupDir);
        Path testFile = backupDir.resolve("2026-08-31_0902_startup.sql");
        Files.writeString(testFile, "-- test backup");

        try {
            mockMvc.perform(get("/backups"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("backup/list"))
                    .andExpect(model().attribute("backups", hasItem(
                            new BackupController.BackupEntry(
                                    "2026-08-31_0902_startup.sql",
                                    LocalDate.of(2026, 8, 31),
                                    LocalTime.of(9, 2),
                                    "Uygulama açılışı"))));
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    void restore_invalidFileName_setsErrorMessageFlashAndRedirects() throws Exception {
        mockMvc.perform(post("/backups/restore/{fileName}", "..backup.sql"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backups"))
                .andExpect(flash().attribute("errorMessage", "Geçersiz dosya adı"));

        verifyNoInteractions(backupService);
    }

    @Test
    void restore_validFileName_setsSuccessMessageFlashAndRedirects() throws Exception {
        mockMvc.perform(post("/backups/restore/{fileName}", "2026-08-31_0902_startup.sql"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backups"))
                .andExpect(flash().attribute("successMessage", "Yedek başarıyla geri yüklendi: 2026-08-31_0902_startup.sql"));

        verify(backupService).restore(any(File.class));
    }

    @Test
    void restore_serviceThrows_setsErrorMessageFlashAndRedirects() throws Exception {
        doThrow(new IllegalStateException("Geri yükleme başarısız oldu, çıkış kodu: 1"))
                .when(backupService).restore(any(File.class));

        mockMvc.perform(post("/backups/restore/{fileName}", "2026-08-31_0902_startup.sql"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backups"))
                .andExpect(flash().attribute("errorMessage", "Geri yükleme başarısız oldu, çıkış kodu: 1"));
    }

    @Test
    void reset_success_setsSuccessMessageFlashAndRedirects() throws Exception {
        mockMvc.perform(post("/backups/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backups"))
                .andExpect(flash().attribute("successMessage", "Veritabanı sıfırlandı."));

        verify(backupService).resetDatabase();
    }

    @Test
    void reset_serviceThrows_setsErrorMessageFlashAndRedirects() throws Exception {
        doThrow(new IllegalStateException("Sıfırlama öncesi güvenlik yedeği alınamadı"))
                .when(backupService).resetDatabase();

        mockMvc.perform(post("/backups/reset"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/backups"))
                .andExpect(flash().attribute("errorMessage", "Sıfırlama öncesi güvenlik yedeği alınamadı"));
    }
}