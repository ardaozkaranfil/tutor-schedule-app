package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.ClassGroup;
import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.ClassGroupRepository;
import com.tutorschedule.app.repository.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles CRUD and search operations on student records.
 */
@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;

    public StudentService(StudentRepository studentRepository, ClassGroupRepository classGroupRepository){
        this.studentRepository = studentRepository;
        this.classGroupRepository = classGroupRepository;
    }

    /**
     * Returns every student in the system.
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Fetches the student with the given number; throws
     * IllegalArgumentException if none exists.
     */
    public Student getStudentById(Long id){
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Öğrenci bulunamadı: " + id));
    }

    /**
     * Searches by name, case-insensitively. Returns every student if name
     * is null or blank.
     */
    public List<Student> searchStudents(String name){
        boolean hasName = name != null && !name.isEmpty();

        if(hasName){
            return studentRepository.findByFullNameContainingIgnoreCase(name);
        }

        return studentRepository.findAll();
    }

    /**
     * Saves a new student. Since id doubles as the school number and isn't
     * auto-generated, an already-registered number throws
     * IllegalArgumentException.
     */
    @Transactional
    public Student createStudent(Long id, String className, String fullName){
        if (studentRepository.existsById(id)) {
            throw new IllegalArgumentException("Bu numara zaten kayıtlı: " + id);
        }
        Student student = new Student();
        student.setId(id);
        student.setClassName(className);
        student.setFullName(fullName);

        if (!classGroupRepository.existsById(className)) {
            classGroupRepository.save(new ClassGroup(className));
        }
        return studentRepository.save(student);
    }

    /**
     * Updates an existing student's details. Throws via getStudentById if
     * the record doesn't exist.
     */
    @Transactional
    public Student updateStudent(Long id, String className, String fullName){
        Student existing = getStudentById(id);
        existing.setId(id);
        existing.setClassName(className);
        existing.setFullName(fullName);

        if (!classGroupRepository.existsById(className)) {
            classGroupRepository.save(new ClassGroup(className));
        }
        return studentRepository.save(existing);
    }

    /**
     * Permanently deletes the student.
     */
    @Transactional
    public void deleteStudent(Long id){
        studentRepository.deleteById(id);
    }

}
