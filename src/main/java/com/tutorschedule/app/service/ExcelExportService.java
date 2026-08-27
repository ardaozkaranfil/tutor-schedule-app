package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
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
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces a color-coded Excel (.xlsx) file of a teacher's weekly
 * schedule. FREE is green, BUSY is orange (with the class name shown),
 * BLOCKED is grey. Weekday and weekend schedules are rendered as two
 * separate, independent tables side by side (since their time slots
 * can differ), with one blank column between them.
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

    public ExcelExportService(TeacherRepository teacherRepository,
                              ScheduleService scheduleService,
                              TimeSlotRepository timeSlotRepository) {
        this.teacherRepository = teacherRepository;
        this.scheduleService = scheduleService;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Builds the teacher's weekly schedule into a single-sheet Excel file
     * with two side-by-side tables (weekday and weekend, since their time
     * slots can differ) and returns it as a byte array. Throws
     * IllegalArgumentException if the teacher doesn't exist.
     */
    public byte[] exportTeacherSchedule(Long teacherId) {
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

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Haftalık Program");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle freeStyle = createColoredStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle busyStyle = createColoredStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle blockedStyle = createColoredStyle(workbook, IndexedColors.GREY_25_PERCENT);

            writeTableTitle(sheet, titleStyle, "Hafta İçi Programı", WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS.length);
            writeTableTitle(sheet, titleStyle, "Hafta Sonu Programı", WEEKEND_TABLE_START_COL, WEEKEND_DAYS.length);

            writeTableHeader(sheet, headerStyle, WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS);
            writeTableHeader(sheet, headerStyle, WEEKEND_TABLE_START_COL, WEEKEND_DAYS);

            writeTableBody(sheet, WEEKDAY_TABLE_START_COL, WEEKDAY_DAYS, weekdaySlots, byDayAndSlot,
                    freeStyle, busyStyle, blockedStyle);
            writeTableBody(sheet, WEEKEND_TABLE_START_COL, WEEKEND_DAYS, weekendSlots, byDayAndSlot,
                    freeStyle, busyStyle, blockedStyle);

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
     * Returns a filesystem-safe "TeacherName_yyyy-MM-dd" base for the
     * export file name (no extension), used by the controller to build
     * the Content-Disposition header.
     */
    public String buildExportFileBaseName(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Öğretmen bulunamadı: " + teacherId));
        String safeName = teacher.getFullName().trim().replaceAll("\\s+", "_");
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return safeName + "_" + date;
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
     * Writes a merged title cell ("Haftaiçi Programı" / "Haftasonu
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
     */
    private void writeTableBody(Sheet sheet, int startCol, DayOfWeek[] days, List<TimeSlot> slots,
                                Map<DayOfWeek, Map<Long, TeacherSchedule>> byDayAndSlot,
                                CellStyle freeStyle, CellStyle busyStyle, CellStyle blockedStyle) {
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
     * Formats a time slot as "14:00-14:40".
     */
    private String formatTimeRange(TimeSlot slot) {
        return slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER);
    }

    /**
     * Builds a bold, larger-font cell style used for the "Haftaiçi
     * Programı" / "Haftasonu Programı" section titles.
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
     * tell FREE/BUSY/BLOCKED apart.
     */
    private CellStyle createColoredStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}