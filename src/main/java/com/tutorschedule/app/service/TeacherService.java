package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Teacher;
import com.tutorschedule.app.entity.TeacherSchedule;
import com.tutorschedule.app.entity.TeacherScheduleStatus;
import com.tutorschedule.app.entity.TimeSlot;
import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.repository.TeacherRepository;
import com.tutorschedule.app.repository.TeacherScheduleRepository;
import com.tutorschedule.app.repository.TimeSlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeacherService {

    private static final List<DayOfWeek> SCHOOL_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    );

    private final TeacherRepository teacherRepository;
    private final TeacherScheduleRepository teacherScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          TeacherScheduleRepository teacherScheduleRepository,
                          TimeSlotRepository timeSlotRepository) {
        this.teacherRepository = teacherRepository;
        this.teacherScheduleRepository = teacherScheduleRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public List<String> getDistinctBranches() {
        return teacherRepository.findDistinctBranches();
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + id));
    }

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
                    : TimeSlotDayType.WEEKDAY;            for (TimeSlot slot : timeSlotRepository.findByDayTypeOrderByStartTimeAsc(dayType)) {
                String key = scheduleKey(day, slot.getId());
                if (!provided.containsKey(key)) {
                    toSave.add(new TeacherSchedule(saved.getId(), slot.getId(), null, day, TeacherScheduleStatus.FREE));
                }
            }
        }

        teacherScheduleRepository.saveAll(toSave);
        return saved;
    }

    @Transactional
    public Teacher updateTeacher(Long id, String name, String branch) {
        Teacher existing = getTeacherById(id);
        existing.setFullName(name);
        existing.setBranch(branch);
        return teacherRepository.save(existing);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        teacherScheduleRepository.deleteAll(teacherScheduleRepository.findByTeacherId(id));
        teacherRepository.deleteById(id);
    }

    private String scheduleKey(DayOfWeek dayOfWeek, Long timeSlotId) {
        return dayOfWeek + "-" + timeSlotId;
    }
}