# Tutor Schedule App

[![CI](https://github.com/ardaozkaranfil/tutor-schedule-app/actions/workflows/ci.yml/badge.svg)](https://github.com/ardaozkaranfil/tutor-schedule-app/actions/workflows/ci.yml)

A scheduling and appointment system built for a cram school. It replaces a paper weekly schedule with a local web app used by the guidance counselor to manage teacher availability and book one-on-one student sessions.

## Status

Finished and in active use — currently running at 2 tutoring centers. Built between July–August 2026 alongside coursework as a self-directed summer project.

## What it does

- Guidance counselor manages the teacher list, including subject/branch info.
- Teacher weekly availability is entered manually and stored — which slots are busy (with which class), free, or blocked (teacher off that day/hour).
- Weekday and weekend hours can differ.
- Student records are imported from an Excel file (name, course number, class), and classes are created automatically from that data.
- One-on-one appointments are booked against a real calendar date, checked against the teacher's weekly grid, with conflict prevention — no double-booking the same slot.
- Appointments can be cancelled.
- Weekly and daily teacher schedules can be exported to Excel in a printable layout.
- Teachers and students can be added in bulk.
- Database backups are taken automatically (on app startup and whenever a schedule is saved) and can be restored from a saved dump.
- Class groups can be created on the fly from the student add/edit form, not just via Excel import.
- Student and teacher lists have a name search bar.
- The database can be reset for a new school year (triple-confirmation to prevent accidental data loss), with old backups retained.
- Runs locally, on the guidance counselor's machine — not deployed to the internet. Each tutoring center using it runs its own independent local install; there's no shared or central server between them.

## Tech stack

- Java 21
- Spring Boot 3.5.16
- Spring Data JPA / Hibernate
- Thymeleaf
- MySQL
- Apache POI (Excel import/export)
- JUnit 5 / Mockito (unit tests), H2 (in-memory test database), JaCoCo (coverage)
- Postman (manual API testing/collection)
- Docker (app + MySQL containers via docker-compose)

## Data model

![ER Diagram](docs/erd.png)

Six entities: `Teacher`, `Student`, `ClassGroup`, `TimeSlot`, `TeacherSchedule`, `Appointment`. `ClassGroup` uses the class name itself as its primary key since class names (e.g. `12-D MF`) come directly from the Excel import rather than being generated. `TeacherSchedule` links a teacher to a time slot and, when occupied, to the class being taught at that time — separate from `Appointment`, which represents a single one-on-one session tied to a specific calendar date.

## Running it — for the guidance counselor

No installation, no commands. Double-click `run.bat`. It brings up the app and its MySQL database as Docker containers (`docker compose up -d`), waits until the app responds, then opens it in the browser automatically at `http://localhost:8080/appointments`.

## Design

Early UI planning is in [`docs/ui-mockup.pdf`](docs/ui-mockup.pdf) — a design-stage mockup, not a screenshot of the working app. Real screenshots of the finished app are coming soon.

## Running it — for development

**Option A — Docker (matches production/`run.bat`):**
1. Clone the repo.
2. Copy `.env.example` to `.env` and set `DB_NAME` / `DB_PASSWORD`.
3. Run:
   ```
   docker compose up -d
   ```
4. Open `http://localhost:8080/appointments`.

**Option B — local MySQL (faster edit/rebuild loop):**
1. Clone the repo.
2. Create a MySQL database:
   ```sql
   CREATE DATABASE tutor_schedule_db;
   ```
3. Copy `application.properties.example` to `src/main/resources/application.properties` and fill in your local MySQL username and password.
4. Run:
   ```
   ./mvnw spring-boot:run
   ```
5. Open `http://localhost:8080/appointments`.

## Testing

The service layer has unit test coverage using JUnit 5 and Mockito — repositories and collaborating services are mocked, so these tests exercise business logic in isolation (validation rules, schedule/availability calculations, Excel import/export mapping, etc.) without touching a real database:

- `StudentService`, `TeacherService`, `TimeSlotService`, `ScheduleService`
- `ScheduleAvailabilityService`, `AppointmentService`
- `ExcelImportService`, `ExcelExportService` (using real in-memory Apache POI workbooks, not mocked)
- `BackupService` (covers the `restore()` guard clause only — the actual `mysqldump`/`mysql` process execution isn't unit-testable as currently structured)

Tests run against an in-memory H2 database (configured in `src/test/resources/application.properties`), so no local MySQL connection is needed:

```
./mvnw test
```

Every push and pull request to `main` also runs this via GitHub Actions (see `.github/workflows/ci.yml`).

Test coverage is measured with JaCoCo. Running `./mvnw test` also generates an HTML report at `target/site/jacoco/index.html` (not committed — regenerate it locally). Current coverage on the service layer, where the unit tests are focused, is ~78% instructions / ~80% branches.

### API testing with Postman

A Postman collection covering the app's JSON/file-download endpoints (student/teacher search, single-student lookup, Excel schedule export) is at `postman/tutor-schedule-app.postman_collection.json`.

To use it:
1. Import the collection into Postman.
2. The `baseUrl` collection variable is preset to `http://localhost:8080` — update it if the app runs on a different port.
3. Start the app (see "Running it — for development" above), then send any request in the collection.

Note: the Postman collection only covers the app's JSON/file-download endpoints (search, lookup, export) — the rest of the create/edit flows go through the Thymeleaf UI.

## Notes

- `application.properties` is gitignored — it holds real database credentials and should never be committed. Use `application.properties.example` as a template.
- No user data (student names, course numbers) is committed to this repo. The app is tested with placeholder data.
- CI runs on GitHub Actions against Java 21; see the badge above or the Actions tab for build status.
- Backups are timestamped in the app's container timezone (Europe/Istanbul), so file names reflect local time regardless of the host machine.