package com.tutorschedule.app.service;

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
 * BLOCKED is grey.
 */
@Service
public class ExcelExportService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final DayOfWeek[] DISPLAY_DAYS = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    };

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Monday",
            DayOfWeek.TUESDAY, "Tuesday",
            DayOfWeek.WEDNESDAY, "Wednesday",
            DayOfWeek.THURSDAY, "Thursday",
            DayOfWeek.FRIDAY, "Friday",
            DayOfWeek.SATURDAY, "Saturday",
            DayOfWeek.SUNDAY, "Sunday"
    );

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
     * Builds the teacher's weekly schedule into a single-sheet Excel file —
     * rows are time slots, columns are Monday through Sunday — and returns
     * it as a byte array. Throws IllegalArgumentException if the teacher
     * doesn't exist. Weekday and weekend slot counts can differ, so the
     * shorter side gets its leftover cells filled with "-".
     */
    public byte[] exportTeacherSchedule(Long teacherId) {
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));

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
        int rowCount = Math.max(weekdaySlots.size(), weekendSlots.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Weekly Schedule");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle freeStyle = createColoredStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle busyStyle = createColoredStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle blockedStyle = createColoredStyle(workbook, IndexedColors.GREY_25_PERCENT);

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Time");
            for (int col = 0; col < DISPLAY_DAYS.length; col++) {
                Cell cell = header.createCell(col + 1);
                cell.setCellValue(DAY_NAMES.get(DISPLAY_DAYS[col]));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                TimeSlot labelSlot = rowIdx < weekdaySlots.size() ? weekdaySlots.get(rowIdx) : weekendSlots.get(rowIdx);
                row.createCell(0).setCellValue(formatTimeRange(labelSlot));

                for (int col = 0; col < DISPLAY_DAYS.length; col++) {
                    DayOfWeek day = DISPLAY_DAYS[col];
                    boolean isWeekendColumn = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                    List<TimeSlot> slotsForColumn = isWeekendColumn ? weekendSlots : weekdaySlots;

                    Cell cell = row.createCell(col + 1);
                    if (rowIdx >= slotsForColumn.size()) {
                        cell.setCellValue("-");
                        continue;
                    }

                    TimeSlot slot = slotsForColumn.get(rowIdx);
                    TeacherSchedule entry = byDayAndSlot.getOrDefault(day, Map.of()).get(slot.getId());

                    if (entry == null) {
                        cell.setCellValue("-");
                        continue;
                    }

                    switch (entry.getStatus()) {
                        case FREE -> {
                            cell.setCellValue("Free");
                            cell.setCellStyle(freeStyle);
                        }
                        case BUSY -> {
                            cell.setCellValue(entry.getClassName());
                            cell.setCellStyle(busyStyle);
                        }
                        case BLOCKED -> {
                            cell.setCellValue("Blocked");
                            cell.setCellStyle(blockedStyle);
                        }
                    }
                }
            }

            for (int col = 0; col <= DISPLAY_DAYS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Formats a time slot as "14:00-14:40".
     */
    private String formatTimeRange(TimeSlot slot) {
        return slot.getStartTime().format(TIME_FORMATTER) + "-" + slot.getEndTime().format(TIME_FORMATTER);
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