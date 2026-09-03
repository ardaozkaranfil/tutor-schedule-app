# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-09-03

### Added
- Appointment history lookup by student or teacher: new `GET /appointments/history`
  endpoint returning a full booking history (upcoming and past, cancelled rows
  included) as JSON, with a summary of total and cancelled counts.
- "Geçmiş randevular" tab on the appointments screen. The upcoming-appointments
  section is now split into "Yaklaşan randevular" (unchanged server-rendered table)
  and "Geçmiş randevular", which pulls a selected student's or teacher's history via
  fetch. Student mode shows date, time, teacher and branch; teacher mode shows date,
  time, student and class. Cancelled rows are badged instead of hidden.

### Changed
- Students are now keyed by a database-generated surrogate id (`IDENTITY`) instead
  of their STOYS school number. STOYS occasionally issues duplicate numbers, which
  tripped the "Bu numara zaten kayıtlı" guard and aborted the Excel import.
- The STOYS Excel export now needs only two columns — student name and class, in
  that order. The number column is no longer read.
- The student add form no longer has a "Kurs No" field, and the student list
  column header is now just "No".

### Removed
- The student number field on the `Student` entity, its import path, and the
  duplicate/invalid-number validation that depended on it.

### Fixed
- Database reset (`resetDatabase()`) now issues `TRUNCATE TABLE` with lowercase
  table names, matching what Hibernate actually creates. On case-sensitive MySQL
  (the Linux `mysql:8` container) the reset previously hit non-existent tables and
  aborted with "çıkış kodu: 1"; it only worked on case-insensitive setups.
- The "Ders programları" page no longer returns HTTP 500 on a fresh setup that has
  time slots but no teachers. The weekly grid blocks are guarded, and a hint
  pointing to the Teachers page is shown when there is no teacher to select.

### Internal
- Test coverage for the appointment-history feature across the repository, service
  and controller layers; student tests adjusted for the generated id.

### Upgrading from 1.0.0
- On each live MySQL database, run once:
  ```sql
  ALTER TABLE student MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
  ```
- Update the STOYS export template used for student import to two columns:
  name, then class. Remove the number column.

## [1.0.0] - 2026-08-31

### Added

- Initial release.

[1.1.0]: https://github.com/ardaozkaranfil/tutor-schedule-app/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ardaozkaranfil/tutor-schedule-app/releases/tag/v1.0.0
