package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.ExcelExportService;
import com.tutorschedule.app.service.ScheduleService;
import com.tutorschedule.app.service.TeacherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the "Ders programı" screen: showing a teacher's weekly schedule
 * grid, updating individual cells (free/busy/blocked), and exporting the
 * schedule to Excel. Reading/writing the template itself is delegated to
 * {@link ScheduleService} so the documented business rules (backup on
 * save, class-must-exist check) are always applied.
 */
@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TeacherService teacherService;
    private final ExcelExportService excelExportService;
    private final TimeSlotRepository timeSlotRepository;

    public ScheduleController(ScheduleService scheduleService,
                              TeacherService teacherService,
                              ExcelExportService excelExportService,
                              TimeSlotRepository timeSlotRepository) {
        this.scheduleService = scheduleService;
        this.teacherService = teacherService;
        this.excelExportService = excelExportService;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Shows the weekly schedule grid for the selected teacher (or the
     * first teacher in the system if none was selected yet).
     */
    @GetMapping
    public String showSchedule(@RequestParam(required = false) Long teacherId, Model model) {
        List<Teacher> teachers = teacherService.getAllTeachers();
        model.addAttribute("teachers", teachers);

        Teacher selectedTeacher = resolveSelectedTeacher(teacherId, teachers);
        model.addAttribute("selectedTeacher", selectedTeacher);

        if (selectedTeacher != null) {
            Map<DayOfWeek, List<TeacherSchedule>> weeklySchedule =
                    scheduleService.getWeeklySchedule(selectedTeacher.getId());
            model.addAttribute("scheduleGrid", buildScheduleGrid(weeklySchedule));
        }

        model.addAttribute("weekdaySlots", timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY));
        model.addAttribute("weekendSlots", timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND));
        model.addAttribute("weekdayDays", List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        model.addAttribute("weekendDays", List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        return "schedule/grid";
    }

    /**
     * Updates a single grid cell for the selected teacher. Validation
     * errors (missing/unknown class name) come back from the service and
     * are shown as a flash error instead of a raw exception.
     */
    @PostMapping("/update")
    public String updateCell(
            @RequestParam Long teacherId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam Long timeSlotId,
            @RequestParam TeacherScheduleStatus status,
            @RequestParam(required = false) String className,
            RedirectAttributes redirectAttributes) {

        try {
            scheduleService.updateScheduleEntry(teacherId, dayOfWeek, timeSlotId, status, className);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/schedule?teacherId=" + teacherId;
    }

    /**
     * Downloads the selected teacher's weekly schedule as an .xlsx file.
     * <p>
     * The file contains the recurring free/busy/blocked template plus the
     * one-on-one appointments booked for a single calendar week. That week
     * is the Monday–Sunday range containing {@code date}; when {@code date}
     * is omitted the current week is used. The date only selects which
     * appointments are pulled in — the template grid itself is not
     * date-specific.
     * <p>
     * File name is "&lt;TeacherName&gt;_&lt;WeekMonday-yyyy-MM-dd&gt;_haftasi.xlsx",
     * so it reflects the exported week rather than the download date.
     */
    @GetMapping("/export/{teacherId}")
    @ResponseBody
    public ResponseEntity<byte[]> exportSchedule(
            @PathVariable Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        LocalDate weekReference = (date != null) ? date : LocalDate.now();

        byte[] excelBytes = excelExportService.exportTeacherSchedule(teacherId, weekReference);
        String baseName = excelExportService.buildExportFileBaseName(teacherId, weekReference);
        String fileName = baseName + ".xlsx";
        String asciiFileName = excelExportService.toAsciiFallback(fileName);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(excelBytes);
    }

    /**
     * Picks the teacher whose schedule should be shown: the requested one
     * if given and valid, otherwise the first teacher in the list, or null
     * if there are no teachers at all yet.
     */
    private Teacher resolveSelectedTeacher(Long teacherId, List<Teacher> teachers) {
        if (teacherId != null) {
            return teacherService.getTeacherById(teacherId);
        }
        return teachers.isEmpty() ? null : teachers.get(0);
    }

    /**
     * Reshapes the day-grouped weekly schedule into timeSlotId -> (day -> entry),
     * so the grid template can look up a single cell with
     * scheduleGrid.get(slot.id)?.get(day) instead of filtering a list per cell.
     */
    private Map<Long, Map<DayOfWeek, TeacherSchedule>> buildScheduleGrid(
            Map<DayOfWeek, List<TeacherSchedule>> weeklySchedule) {
        Map<Long, Map<DayOfWeek, TeacherSchedule>> grid = new HashMap<>();
        for (Map.Entry<DayOfWeek, List<TeacherSchedule>> dayEntry : weeklySchedule.entrySet()) {
            DayOfWeek day = dayEntry.getKey();
            for (TeacherSchedule entry : dayEntry.getValue()) {
                grid.computeIfAbsent(entry.getTimeSlotId(), id -> new EnumMap<>(DayOfWeek.class))
                        .put(day, entry);
            }
        }
        return grid;
    }
}