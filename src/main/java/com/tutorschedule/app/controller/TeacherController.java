package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.TeacherService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the "Öğretmenler" screens: listing/searching teachers, adding a
 * new teacher (together with their initial weekly schedule), editing a
 * teacher's name/branch, and deleting a teacher. All mutating operations
 * are delegated to {@link TeacherService} so the documented business rules
 * (weekly schedule template on creation, cascading schedule cleanup on
 * delete, backup on creation) are always applied — this controller never
 * touches the repositories directly.
 */
@Controller
@RequestMapping("/teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TimeSlotRepository timeSlotRepository;

    public TeacherController(TeacherService teacherService, TimeSlotRepository timeSlotRepository) {
        this.teacherService = teacherService;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Lists teachers, optionally filtered by name and/or branch, and feeds
     * the branch dropdown used on the filter bar.
     */
    @GetMapping
    public String listTeachers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String branch,
            Model model) {

        model.addAttribute("teachers", teacherService.searchTeachers(name, branch));
        model.addAttribute("branches", teacherService.getDistinctBranches());
        model.addAttribute("selectedBranch", branch);
        return "teacher-list";
    }

    /**
     * Deletes a teacher along with their weekly schedule rows.
     */
    @PostMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return "redirect:/teachers";
    }

    /**
     * Shows the add-teacher form together with an empty weekly schedule
     * grid (one row per defined time slot) that can optionally be filled
     * in before saving.
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("teacher", new Teacher());
        model.addAttribute("branches", teacherService.getDistinctBranches());
        addWeeklyGridAttributes(model);
        return "add-teacher";
    }

    /**
     * Shows the edit form for an existing teacher. Only name and branch are
     * editable here — the weekly schedule itself is edited from the "Ders
     * programı" screen, not this one.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("teacher", teacherService.getTeacherById(id));
        model.addAttribute("branches", teacherService.getDistinctBranches());
        return "add-teacher";
    }

    /**
     * Creates the teacher and, if any weekly grid cells were filled in on
     * the form, saves them too — any cell left untouched is defaulted to
     * FREE by {@link TeacherService#createTeacher}. Triggers a backup.
     */
    @PostMapping("/add")
    public String submitAddForm(
            @RequestParam String fullName,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) List<Long> timeSlotId,
            @RequestParam(required = false) List<DayOfWeek> dayOfWeek,
            @RequestParam(required = false) List<TeacherScheduleStatus> status,
            @RequestParam(required = false) List<String> className) {

        List<TeacherSchedule> scheduleEntries = buildScheduleEntries(timeSlotId, dayOfWeek, status, className);
        teacherService.createTeacher(fullName, branch, scheduleEntries);
        return "redirect:/teachers";
    }

    /**
     * Updates a teacher's name and branch.
     */
    @PostMapping("/edit/{id}")
    public String submitEditForm(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) String branch) {

        teacherService.updateTeacher(id, fullName, branch);
        return "redirect:/teachers";
    }

    /**
     * Adds the weekday/weekend time slot lists the add-teacher grid is
     * rendered from (rows = time slots, columns = days).
     */
    private void addWeeklyGridAttributes(Model model) {
        List<TimeSlot> weekdaySlots = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKDAY);
        List<TimeSlot> weekendSlots = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(TimeSlotDayType.WEEKEND);
        model.addAttribute("weekdaySlots", weekdaySlots);
        model.addAttribute("weekendSlots", weekendSlots);
        model.addAttribute("weekdayDays", List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        model.addAttribute("weekendDays", List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    /**
     * Zips the parallel form arrays (one entry per filled-in grid cell)
     * into {@link TeacherSchedule} rows. teacherId is left unset here —
     * {@link TeacherService#createTeacher} fills it in once the teacher is
     * saved and an id is assigned.
     */
    private List<TeacherSchedule> buildScheduleEntries(
            List<Long> timeSlotIds, List<DayOfWeek> daysOfWeek,
            List<TeacherScheduleStatus> statuses, List<String> classNames) {

        if (timeSlotIds == null || timeSlotIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TeacherSchedule> entries = new ArrayList<>();
        for (int i = 0; i < timeSlotIds.size(); i++) {
            String cellClassName = (classNames != null && i < classNames.size()) ? classNames.get(i) : null;
            entries.add(new TeacherSchedule(
                    null,
                    timeSlotIds.get(i),
                    cellClassName,
                    daysOfWeek.get(i),
                    statuses.get(i)));
        }
        return entries;
    }
}