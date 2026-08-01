package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Appointment;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TimeSlotRepository;
import com.tutorschedule.app.service.AppointmentService;
import com.tutorschedule.app.service.ScheduleAvailabilityService;
import com.tutorschedule.app.service.StudentService;
import com.tutorschedule.app.service.TeacherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the "Randevular" screen: showing a single teacher's day
 * availability or a whole branch's comparison grid, booking a new 1:1
 * appointment on a free slot, and cancelling an existing one. Availability
 * itself is never computed here — that's delegated to
 * {@link ScheduleAvailabilityService} so both this screen and the
 * "Ders programı" screen stay in sync.
 */
@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ScheduleAvailabilityService scheduleAvailabilityService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final TimeSlotRepository timeSlotRepository;

    public AppointmentController(AppointmentService appointmentService,
                                 ScheduleAvailabilityService scheduleAvailabilityService,
                                 TeacherService teacherService,
                                 StudentService studentService,
                                 TimeSlotRepository timeSlotRepository) {
        this.appointmentService = appointmentService;
        this.scheduleAvailabilityService = scheduleAvailabilityService;
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Shows the "Randevular" screen. If a specific teacher is selected,
     * renders that teacher's single-day grid (calendar.html); if only a
     * branch is selected (teacherId left as "hepsi"), renders the
     * branch-wide comparison grid (compare.html) built by calling
     * ScheduleAvailabilityService once per teacher in that branch.
     */
    @GetMapping
    public String showAppointments(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) Long teacherId,
            Model model) {

        LocalDate selectedDate = (date != null) ? date : LocalDate.now();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("branches", teacherService.getDistinctBranches());
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("selectedTeacherId", teacherId);
        model.addAttribute("upcomingAppointments", buildUpcomingAppointmentRows());

        if (teacherId != null) {
            Teacher teacher = teacherService.getTeacherById(teacherId);
            model.addAttribute("teacher", teacher);
            model.addAttribute("dayAvailability", scheduleAvailabilityService.getTeacherDayAvailability(teacher, selectedDate));
            model.addAttribute("teachers", teacherService.getAllTeachers());
            return "appointment/calendar";
        }

        List<Teacher> branchTeachers = (branch != null && !branch.isEmpty())
                ? teacherService.searchTeachers(null, branch)
                : teacherService.getAllTeachers();

        Map<Teacher, Map<TimeSlot, TeacherScheduleStatus>> branchAvailability = new LinkedHashMap<>();
        for (Teacher teacher : branchTeachers) {
            branchAvailability.put(teacher, scheduleAvailabilityService.getTeacherDayAvailability(teacher, selectedDate));
        }

        DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();
        TimeSlotDayType dayType = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
                ? TimeSlotDayType.WEEKEND
                : TimeSlotDayType.WEEKDAY;

        model.addAttribute("teachers", branchTeachers);
        model.addAttribute("daySlots", timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType));
        model.addAttribute("branchAvailability", branchAvailability);
        return "appointment/compare";
    }

    /**
     * Books a new 1:1 appointment on a free slot. teacherId/timeSlotId
     * identify the clicked cell; studentId comes from the student the user
     * picked via the search box (not typed freely — createAppointment needs
     * an actual id). Validation errors (slot no longer available, unknown
     * teacher/student) are shown as a flash error instead of a raw
     * exception.
     */
    @PostMapping("/book")
    public String bookAppointment(
            @RequestParam Long teacherId,
            @RequestParam Long timeSlotId,
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointmentDate,
            @RequestParam(required = false) String branch,
            RedirectAttributes redirectAttributes) {

        try {
            appointmentService.createAppointment(teacherId, timeSlotId, studentId, appointmentDate);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/appointments?date=" + appointmentDate + (branch != null ? "&branch=" + branch : "");
    }

    /**
     * Cancels an existing appointment (flips its status, doesn't delete it).
     */
    @PostMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return "redirect:/appointments";
    }

    /**
     * Builds the "Yaklaşan randevular" rows. Appointment only stores raw
     * ids, so teacher/student names are resolved here for display instead
     * of leaking id-lookups into the template.
     */
    private List<UpcomingAppointmentRow> buildUpcomingAppointmentRows() {
        return appointmentService.getAppointments().stream()
                .map(this::toUpcomingRow)
                .toList();
    }

    private UpcomingAppointmentRow toUpcomingRow(Appointment appointment) {
        Teacher teacher = teacherService.getTeacherById(appointment.getTeacherId());
        Student student = studentService.getStudentById(appointment.getStudentId());
        return new UpcomingAppointmentRow(
                appointment.getId(),
                appointment.getAppointmentDate(),
                teacher.getFullName(),
                student.getFullName());
    }

    /**
     * Display-only row for the upcoming appointments table — keeps the
     * template from having to call back into services for names.
     */
    private record UpcomingAppointmentRow(Long id, LocalDate date, String teacherName, String studentName) {
    }
}