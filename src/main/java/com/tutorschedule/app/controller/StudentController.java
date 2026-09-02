package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.ClassGroupRepository;
import com.tutorschedule.app.service.ExcelImportService;
import com.tutorschedule.app.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles the "Öğrenciler" screens: listing/searching students, adding a
 * student manually or via bulk Excel import, editing a student's name/class,
 * and deleting a student. All mutating operations are delegated to
 * {@link StudentService} (and {@link ExcelImportService} for bulk import) so
 * the documented business rules (database-generated id, backup on import) are
 * always applied — this controller never touches the repositories directly
 * except {@link ClassGroupRepository} for feeding the class dropdown.
 */
@Controller
@RequestMapping("/students")
public class StudentController {
    private final StudentService studentService;
    private final ExcelImportService excelImportService;
    private final ClassGroupRepository classGroupRepository;

    public StudentController(StudentService studentService, ExcelImportService excelImportService, ClassGroupRepository classGroupRepository){
        this.studentService = studentService;
        this.excelImportService = excelImportService;
        this.classGroupRepository = classGroupRepository;
    }

    /**
     * Lists students, optionally filtered by name, for the "Öğrenciler" page.
     * Also feeds the class dropdown used by the inline "Öğrenci ekle" box,
     * since there's no separate add page for students (unlike teachers).
     */
    @GetMapping
    public String listStudents(@RequestParam(required = false) String name, Model model){
        model.addAttribute("students", studentService.searchStudents(name));
        model.addAttribute("classGroups", classGroupRepository.findAll());
        return "student/list";
    }

    /**
     * Live-search endpoint for the "Öğrenciler" filter bar. Returns the same
     * filtered result as {@link #listStudents}, but as JSON instead of a
     * rendered page — called via fetch() from the search box on each keystroke
     * so the list can update without a full page reload.
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Student> searchStudent(@RequestParam(required = false) String name){
        return studentService.searchStudents(name);
    }

    /**
     * Creates a new student. The id is assigned by the database. Does not
     * trigger a backup — single manual entries aren't a backup trigger per
     * the documented rules.
     */
    @PostMapping("/add")
    public String submitAddForm(
            @RequestParam String fullName,
            @RequestParam String className){

        studentService.createStudent(className, fullName);
        return "redirect:/students";
    }

    /**
     * Bulk-imports students from an uploaded Excel file via
     * {@link ExcelImportService}. The resulting summary (how many students were
     * added, how many rows failed) is flashed back to the "Öğrenciler" page; a
     * read failure (e.g. corrupt file) is caught and shown as a flash error
     * instead of a raw exception. A backup is triggered inside the import
     * service itself, not here.
     */
    @PostMapping("/import")
    public String importFromExcel(@RequestParam MultipartFile file, RedirectAttributes redirectAttributes){
        try {
            String summary = excelImportService.importFromExcel(file);
            redirectAttributes.addFlashAttribute("importSummary", summary);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/students";
    }

    /**
     * Permanently deletes a student.
     */
    @PostMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id){
        studentService.deleteStudent(id);
        return "redirect:/students";
    }

    /**
     * Updates a student's class and full name. The school number (id) itself
     * isn't editable here since it's the generated key.
     */
    @PostMapping("/edit/{id}")
    public String editStudent(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String className){

        studentService.updateStudent(id, className, fullName);
        return "redirect:/students";
    }

    /**
     * Live lookup for the "Düzenle" link — returns a single student as JSON
     * so the inline "Öğrenci ekle" box can be pre-filled by JS without a
     * page reload, instead of navigating to a separate edit page.
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Student getStudent(@PathVariable Long id){
        return studentService.getStudentById(id);
    }
}
