package com.tutorschedule.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables methods annotated with @Async to run on separate threads across
 * the app. Currently the only user is BackupService.performBackup — backups
 * run in the background so they don't block the request thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}