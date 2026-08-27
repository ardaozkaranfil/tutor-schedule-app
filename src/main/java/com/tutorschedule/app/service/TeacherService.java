package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.*;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Handles CRUD operations on teacher records and the initial setup of a
 * teacher's weekly schedule template (TeacherSchedule).
 */
@Service
public class TeacherService {

    /**
     * The seven days walked over when building out the weekly schedule template.
     */
    private static final List<DayOfWeek> SCHOOL_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    private final BackupService backupService;
    private final TeacherRepository teacherRepository;
    private final TeacherScheduleRepository teacherScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          TeacherScheduleRepository teacherScheduleRepository,
                          TimeSlotRepository timeSlotRepository,
                          BackupService backupService) {
        this.teacherRepository = teacherRepository;
        this.teacherScheduleRepository = teacherScheduleRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.backupService = backupService;
    }

    /**
     * Returns every teacher in the system.
     */
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    /**
     * Returns the list of distinct branches in use among registered teachers.
     */
    public List<String> getDistinctBranches() {
        return teacherRepository.findDistinctBranches();
    }

    /**
     * Fetches the teacher with the given id; throws IllegalArgumentException
     * if none exists.
     */
    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Öğretmen bulunamadı: " + id));
    }

    /**
     * Looks up a teacher by id, returning an empty {@link Optional} instead of
     * throwing when none exists. Use this where a missing teacher is an
     * expected, recoverable case — such as rendering appointments that still
     * reference a since-deleted teacher — and {@link #getTeacherById} where
     * absence would be a programming error.
     */
    public Optional<Teacher> findTeacherById(Long id) {
        return teacherRepository.findById(id);
    }

    /**
     * Searches by name and/or branch. If both are given, they're combined;
     * if neither is given, every teacher is returned.
     */
    public List<Teacher> searchTeachers(String name, String branch) {
        boolean hasName = name != null && !name.isEmpty();
        boolean hasBranch = branch != null && !branch.isEmpty();

        if (hasName && hasBranch) {
            return teacherRepository.findByFullNameContainingIgnoreCaseAndBranch(name, branch);
        }
        if (hasName) {
            return teacherRepository.findByFullNameContainingIgnoreCase(name);
        }
        if (hasBranch) {
            return teacherRepository.findByBranch(branch);
        }
        return teacherRepository.findAll();
    }

    /**
     * Saves a new teacher and builds out their weekly schedule template.
     * Whatever schedule rows are passed in (scheduleEntries) are saved as-is;
     * any day/slot combination missing from that input gets an automatic
     * FREE row added — so every teacher ends up with exactly one row per
     * defined time slot. A backup is taken automatically after saving.
     */
    @Transactional
    public Teacher createTeacher(String name, String branch, List<TeacherSchedule> scheduleEntries) {
        Teacher teacher = new Teacher();
        teacher.setFullName(name);
        teacher.setBranch(branch);
        Teacher saved = teacherRepository.save(teacher);

        Map<String, TeacherSchedule> provided = new HashMap<>();
        for (TeacherSchedule entry : scheduleEntries) {
            entry.setTeacherId(saved.getId());
            provided.put(scheduleKey(entry.getDayOfWeek(), entry.getTimeSlotId()), entry);
        }

        List<TeacherSchedule> toSave = new ArrayList<>(provided.values());
        for (DayOfWeek day : SCHOOL_DAYS) {
            TimeSlotDayType dayType = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                    ? TimeSlotDayType.WEEKEND
                    : TimeSlotDayType.WEEKDAY;
            for (TimeSlot slot : timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType)) {
                String key = scheduleKey(day, slot.getId());
                if (!provided.containsKey(key)) {
                    toSave.add(new TeacherSchedule(saved.getId(), slot.getId(), null, day, TeacherScheduleStatus.FREE));
                }
            }
        }

        teacherScheduleRepository.saveAll(toSave);
        backupService.performBackup(BackupTrigger.SCHEDULE_SAVE);
        return saved;
    }

    /**
     * Updates a teacher's name and branch.
     */
    @Transactional
    public Teacher updateTeacher(Long id, String name, String branch) {
        Teacher existing = getTeacherById(id);
        existing.setFullName(name);
        existing.setBranch(branch);
        return teacherRepository.save(existing);
    }

    /**
     * Deletes the teacher along with all of their weekly schedule rows.
     */
    @Transactional
    public void deleteTeacher(Long id) {
        teacherScheduleRepository.deleteAll(teacherScheduleRepository.findByTeacherId(id));
        teacherRepository.deleteById(id);
    }

    /**
     * Builds a lookup key from a day and slot id, used to quickly match
     * against pre-defined schedule rows.
     */
    private String scheduleKey(DayOfWeek dayOfWeek, Long timeSlotId) {
        return dayOfWeek + "-" + timeSlotId;
    }
}