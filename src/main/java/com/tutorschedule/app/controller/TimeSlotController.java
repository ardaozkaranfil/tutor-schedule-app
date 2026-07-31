package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.service.TimeSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

/**
 * Handles the "Zaman ayarları" screen: listing weekday/weekend time slots,
 * adding a new one, editing an existing one's hours, and deleting one. All
 * mutating operations are delegated to {@link TimeSlotService} so the
 * documented business rules (no overlaps, teacher-schedule rows kept in
 * sync) are always applied.
 */
@Controller
@RequestMapping("/timeslots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    /**
     * Shows the "Zaman ayarları" page with weekday and weekend slots listed
     * in two separate columns.
     */
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("weekdaySlots", timeSlotService.getWeekdayTimeSlots());
        model.addAttribute("weekendSlots", timeSlotService.getWeekendTimeSlots());
        return "timeslot/settings";
    }

    /**
     * Adds a new time slot to the weekday or weekend list. Overlap/ordering
     * errors from the service are caught and shown as a flash error instead
     * of a raw exception.
     */
    @PostMapping("/add")
    public String addTimeSlot(
            @RequestParam TimeSlotDayType dayType,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            RedirectAttributes redirectAttributes) {

        try {
            timeSlotService.addTimeSlot(dayType, startTime, endTime);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/timeslots";
    }

    /**
     * Updates an existing time slot's hours (inline edit on the settings
     * page — dayType itself isn't editable here).
     */
    @PostMapping("/edit/{id}")
    public String editTimeSlot(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            RedirectAttributes redirectAttributes) {

        try {
            timeSlotService.updateTimeSlot(id, startTime, endTime);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/timeslots";
    }

    /**
     * Deletes a time slot along with its teacher-schedule rows.
     */
    @PostMapping("/delete/{id}")
    public String deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return "redirect:/timeslots";
    }
}