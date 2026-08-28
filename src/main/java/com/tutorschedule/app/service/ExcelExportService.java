package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.AppointmentStatus;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.AppointmentRepository;
import com.tutorschedule.app.repository.StudentRepository;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Produces a color-coded Excel (.xlsx) file of a teacher's weekly
 * schedule. FREE is green, BUSY is orange (with the class name shown),
 * BLOCKED is grey, and a slot that has a one-on-one appointment booked
 * for the exported week is blue (with the student's name shown).
 * <p>
 * Weekday and weekend schedules are rendered as two separate, independent
 * tables side by side (since their time slots can differ), with one blank
 * column between them. Below those two tables a summary table lists every
 * appointment booked for the exported week (date, day, time, student,
 * class), sorted by date and start time.
 * <p>
 * The weekly template is keyed by day-of-week only, whereas appointments
 * carry a real date; the export therefore resolves a concrete week
 * (Monday–Sunday) from the caller-supplied reference date and only pulls
 * appointments that fall inside it.
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final DayOfWeek[] WEEKDAY_DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    };

    private static final DayOfWeek[] WEEKEND_DAYS = {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Pazartesi",
            DayOfWeek.TUESDAY, "Salı",
            DayOfWeek.WEDNESDAY, "Çarşamba",
            DayOfWeek.THURSDAY, "Perşembe",
            DayOfWeek.FRIDAY, "Cuma",
            DayOfWeek.SATURDAY, "Cumartesi",
            DayOfWeek.SUNDAY, "Pazar"
    );

    // Weekday table occupies columns 0..5 (Saat + 5 days).
    private static final int WEEKDAY_TABLE_START_COL = 0;
    // Column 6 is left blank as a gap between the two tables.
    private static final int WEEKEND_TABLE_START_COL = 7;

    private final TeacherRepository teacherRepository;
    private final ScheduleService scheduleService;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final StudentRepository studentRepository;

    public ExcelExportService(TeacherRepository teacherRepository,
                              ScheduleService scheduleService,
                              TimeSlotRepository timeSlotRepository,
                              AppointmentRepository appointmentRepository,
                              StudentRepository studentRepository) {
        this.teacherRepository = teacherRepository;
        this.scheduleService = scheduleService;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Builds the teacher's weekly schedule into a single-sheet Excel file
     * and returns it as a byte array. The sheet holds two side-by-side
     * tables (weekday and weekend, since their time slots can differ),
     * each cell marked FREE/BUSY/BLOCKED, plus any slot that has an active
     * appointment during the resolved week overridden with the student's
     * name. A summary table of that week's appointments is appended below.
     *
     * @param teacherId     the teacher whose schedule is exported
     * @param weekReference any date inside the week to export; the actual
     *                      window is the Monday–Sunday containing it
     * @throws IllegalArgumentException if the teacher doesn't exist
     */
    public byte[] exportTeacherSchedule(Long teacherId, LocalDate weekReference) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Öğretmen bulunamadı: " + teacherId));

        Map<DayOfWeek, List<TeacherSchedule>> weeklySchedule = scheduleService.getWeeklySchedule(teacherId);
        Map<DayOfWeek, Map<Long, TeacherSchedule>> byDayAndSlot = new EnumMap<>(DayOfWeek.class);
        for (Map.Entry<DayOfWeek, List<TeacherSchedule>> entry : weeklySchedule.entrySet()) {
            Map<Long, TeacherSchedule> byTimeSlot = new HashMap<>();
            for (TeacherSchedule ts : entry.getValue()) {
                byTimeSlot.put(ts.getTimeSlotId(), ts);
            }
            byDayAndSlot.put(entry.getKey(), byTimeSlot);
        }

        List<TimeSlot> weekdaySlots = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY);
        List<TimeSlot> weekendSlots = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND);

        LocalDate weekStart = weekReference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Appointment> weekAppointments = appointmentRepository
                .findByTeacherIdAndStatusAndAppointmentDateBetween(
                        teacherId, AppointmentStatus.ACTIVE, weekStart, weekEnd);

        Map<DayOfWeek, Map<Long, Appointment>> apptByDayAndSlot = indexAppointmentsByDayAndSlot(weekAppointments);
        Map<Long, String> studentNames = loadStudentNames(weekAppointments);
        Map<Long, TimeSlot> slotById = indexSlotsById(weekdaySlots, weekendSlots);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Haftalık Program");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle freeStyle = createColoredStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle busyStyle = createColoredStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle blockedStyle = createColoredStyle(workbook, IndexedColors.GREY_25_PERCENT);
            CellStyle appointmentStyle = createColoredStyle(workbook, IndexedColors.PALE_BLUE);

            writeTableTitle(sheet, titleStyle, "Hafta İçi Programı", WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS.length);
            writeTableTitle(sheet, titleStyle, "Hafta Sonu Programı", WEEKEND_TABLE_START_COL, WEEKEND_DAYS.length);

            writeTableHeader(sheet, headerStyle, WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS);
            writeTableHeader(sheet, headerStyle, WEEKEND_TABLE_START_COL, WEEKEND_DAYS);

            writeTableBody(sheet, WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS, weekdaySlots, byDayAndSlot,
                    apptByDayAndSlot, studentNames,
                    freeStyle, busyStyle, blockedStyle, appointmentStyle);
            writeTableBody(sheet, WEEKEND_TABLE_START_COL, WEEKEND_DAYS, weekendSlots, byDayAndSlot,
                    apptByDayAndSlot, studentNames,
                    freeStyle, busyStyle, blockedStyle, appointmentStyle);

            int gridBodyRows = Math.max(weekdaySlots.size(), weekendSlots.size());
            writeAppointmentSummary(sheet, gridBodyRows + 3, titleStyle, headerStyle,
                    weekStart, weekEnd, weekAppointments, slotById);

            int lastCol = WEEKEND_TABLE_START_COL + WEEKEND_DAYS.length;
            for (int col = 0; col <= lastCol; col++) {
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a filesystem-safe "TeacherName_yyyy-MM-dd_haftasi" base for the
     * export file name (no extension). The date is the Monday of the exported
     * week (resolved from {@code weekReference}), so the file name matches the
     * appointments actually inside the sheet rather than "today".
     */
    public String buildExportFileBaseName(Long teacherId, LocalDate weekReference) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Öğretmen bulunamadı: " + teacherId));
        String safeName = teacher.getFullName().trim().replaceAll("\\s+", "_");
        LocalDate weekStart = weekReference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String week = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return safeName + "_" + week + "_haftasi";
    }

    /**
     * ASCII-only fallback of the base name for the quoted
     * Content-Disposition filename parameter — HTTP header values must be
     * Latin-1/ASCII, so diacritics are stripped here via Unicode
     * decomposition. The accented original is still sent via the
     * filename* (UTF-8) parameter.
     */
    public String toAsciiFallback(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ı', 'i')
                .replace('İ', 'I');
        return normalized.replaceAll("[^\\x00-\\x7F]", "_");
    }

    /**
     * Groups the week's appointments into day-of-week -> (timeSlotId ->
     * appointment). Within a single week a given day/slot pair maps to at
     * most one active appointment, so a plain map value is enough.
     */
    private Map<DayOfWeek, Map<Long, Appointment>> indexAppointmentsByDayAndSlot(List<Appointment> appointments) {
        Map<DayOfWeek, Map<Long, Appointment>> byDayAndSlot = new EnumMap<>(DayOfWeek.class);
        for (Appointment appointment : appointments) {
            byDayAndSlot
                    .computeIfAbsent(appointment.getAppointmentDate().getDayOfWeek(), day -> new HashMap<>())
                    .put(appointment.getTimeSlotId(), appointment);
        }
        return byDayAndSlot;
    }

    /**
     * Resolves the display name of every student referenced by the week's
     * appointments in a single query, returning studentId -> full name. A
     * student that was deleted while still referenced simply won't appear
     * in the map; callers fall back to a placeholder label.
     */
    private Map<Long, String> loadStudentNames(List<Appointment> appointments) {
        List<Long> studentIds = appointments.stream()
                .map(Appointment::getStudentId)
                .distinct()
                .toList();
        return studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Student::getFullName));
    }

    /**
     * Indexes the already-loaded weekday and weekend slots by id so the
     * appointment summary can print each appointment's time range without
     * extra per-row lookups.
     */
    private Map<Long, TimeSlot> indexSlotsById(List<TimeSlot> weekdaySlots, List<TimeSlot> weekendSlots) {
        Map<Long, TimeSlot> slotById = new HashMap<>();
        for (TimeSlot slot : weekdaySlots) {
            slotById.put(slot.getId(), slot);
        }
        for (TimeSlot slot : weekendSlots) {
            slotById.put(slot.getId(), slot);
        }
        return slotById;
    }

    /**
     * Writes a merged title cell ("Hafta İçi Programı" / "Hafta Sonu
     * Programı") spanning the "Saat" column plus one column per day.
     */
    private void writeTableTitle(Sheet sheet, CellStyle titleStyle, String title, int startCol, int dayCount) {
        Row titleRow = sheet.getRow(0);
        if (titleRow == null) {
            titleRow = sheet.createRow(0);
        }
        Cell titleCell = titleRow.createCell(startCol);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        int endCol = startCol + dayCount; // startCol = "Saat" column, then one column per day
        sheet.addMergedRegion(new CellRangeAddress(0, 0, startCol, endCol));
    }

    /**
     * Writes the "Saat" + day-name header row (row index 1) for one table.
     */
    private void writeTableHeader(Sheet sheet, CellStyle headerStyle, int startCol, DayOfWeek[] days) {
        Row header = sheet.getRow(1);
        if (header == null) {
            header = sheet.createRow(1);
        }
        Cell saatCell = header.createCell(startCol);
        saatCell.setCellValue("Saat");
        saatCell.setCellStyle(headerStyle);

        for (int col = 0; col < days.length; col++) {
            Cell cell = header.createCell(startCol + col + 1);
            cell.setCellValue(DAY_NAMES.get(days[col]));
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Writes the data rows (starting at row index 2) for one table, using
     * only that table's own time slots — weekday and weekend row counts
     * are independent of each other.
     * <p>
     * Cell precedence per day/slot: an active appointment for the exported
     * week wins and shows "Randevu: &lt;student&gt;" in blue; otherwise the
     * weekly template's FREE/BUSY/BLOCKED value is shown; a slot with
     * neither renders "-".
     */
    private void writeTableBody(Sheet sheet, int startCol, DayOfWeek[] days, List<TimeSlot> slots,
                                Map<DayOfWeek, Map<Long, TeacherSchedule>> byDayAndSlot,
                                Map<DayOfWeek, Map<Long, Appointment>> apptByDayAndSlot,
                                Map<Long, String> studentNames,
                                CellStyle freeStyle, CellStyle busyStyle, CellStyle blockedStyle,
                                CellStyle appointmentStyle) {
        for (int rowIdx = 0; rowIdx < slots.size(); rowIdx++) {
            Row row = sheet.getRow(rowIdx + 2);
            if (row == null) {
                row = sheet.createRow(rowIdx + 2);
            }
            TimeSlot slot = slots.get(rowIdx);
            row.createCell(startCol).setCellValue(formatTimeRange(slot));

            for (int col = 0; col < days.length; col++) {
                DayOfWeek day = days[col];
                Cell cell = row.createCell(startCol + col + 1);

                Appointment appointment = apptByDayAndSlot.getOrDefault(day, Map.of()).get(slot.getId());
                if (appointment != null) {
                    String studentName = studentNames.getOrDefault(
                            appointment.getStudentId(), "(silinmiş öğrenci)");
                    cell.setCellValue("Randevu: " + studentName);
                    cell.setCellStyle(appointmentStyle);
                    continue;
                }

                TeacherSchedule entry = byDayAndSlot.getOrDefault(day, Map.of()).get(slot.getId());

                if (entry == null) {
                    cell.setCellValue("-");
                    continue;
                }

                switch (entry.getStatus()) {
                    case FREE -> {
                        cell.setCellValue("Boş");
                        cell.setCellStyle(freeStyle);
                    }
                    case BUSY -> {
                        cell.setCellValue(entry.getClassName());
                        cell.setCellStyle(busyStyle);
                    }
                    case BLOCKED -> {
                        cell.setCellValue("Bloklu");
                        cell.setCellStyle(blockedStyle);
                    }
                }
            }
        }
    }

    /**
     * Appends the "Bu Haftanın Randevuları" section: a title row spanning
     * the exported week, a header row (Tarih / Gün / Saat / Öğrenci /
     * Sınıf), and one row per active appointment sorted by date then start
     * time. A deleted student or an unknown time slot degrades to a
     * placeholder rather than failing the export.
     *
     * @param startRow first row index this section may use (already past
     *                 the two grids, with one blank row of separation)
     */
    private void writeAppointmentSummary(Sheet sheet, int startRow,
                                         CellStyle titleStyle, CellStyle headerStyle,
                                         LocalDate weekStart, LocalDate weekEnd,
                                         List<Appointment> appointments,
                                         Map<Long, TimeSlot> slotById) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Bu Haftanın Randevuları (" + weekStart + " – " + weekEnd + ")");
        titleCell.setCellStyle(titleStyle);

        Row headerRow = sheet.createRow(startRow + 1);
        String[] columns = {"Tarih", "Gün", "Saat", "Öğrenci", "Sınıf"};
        for (int col = 0; col < columns.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(columns[col]);
            cell.setCellStyle(headerStyle);
        }

        List<Appointment> sorted = appointments.stream()
                .sorted(Comparator
                        .comparing(Appointment::getAppointmentDate)
                        .thenComparing(appointment -> slotStartTime(appointment, slotById)))
                .toList();

        int rowIdx = startRow + 2;
        for (Appointment appointment : sorted) {
            Student student = studentRepository.findById(appointment.getStudentId()).orElse(null);
            TimeSlot slot = slotById.get(appointment.getTimeSlotId());

            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(appointment.getAppointmentDate().toString());
            row.createCell(1).setCellValue(DAY_NAMES.get(appointment.getAppointmentDate().getDayOfWeek()));
            row.createCell(2).setCellValue(slot != null ? formatTimeRange(slot) : "");
            row.createCell(3).setCellValue(student != null ? student.getFullName() : "(silinmiş öğrenci)");
            row.createCell(4).setCellValue(student != null ? student.getClassName() : "");
        }
    }

    /**
     * Start time of an appointment's slot, or {@link LocalTime#MIN} when
     * the slot can't be resolved, so summary sorting stays total.
     */
    private LocalTime slotStartTime(Appointment appointment, Map<Long, TimeSlot> slotById) {
        TimeSlot slot = slotById.get(appointment.getTimeSlotId());
        return slot != null ? slot.getStartTime() : LocalTime.MIN;
    }

    /**
     * Formats a time slot as "14:00-14:40".
     */
    private String formatTimeRange(TimeSlot slot) {
        return slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER);
    }

    /**
     * Builds a bold, larger-font cell style used for the "Hafta İçi
     * Programı" / "Hafta Sonu Programı" section titles.
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 13);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    /**
     * Builds a bold cell style used for the day headers.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    /**
     * Builds a solid-fill cell style in the given color; used to visually
     * tell FREE/BUSY/BLOCKED/appointment cells apart.
     */
    private CellStyle createColoredStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}