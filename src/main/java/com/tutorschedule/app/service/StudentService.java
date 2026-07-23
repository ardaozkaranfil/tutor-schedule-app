package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository){
        this.studentRepository = studentRepository;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id){
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
    }

    public List<Student> searchStudents(String name){
        boolean hasName = name != null && !name.isEmpty();

        if(hasName){
            return studentRepository.findByFullNameContainingIgnoreCase(name);
        }

        return studentRepository.findAll();
    }

    @Transactional
    public Student createStudent(Long id, String className, String fullName){
        if (studentRepository.existsById(id)) {
            throw new IllegalArgumentException("This number already exists: " + id);
        }
        Student student = new Student();
        student.setId(id);
        student.setClassName(className);
        student.setFullName(fullName);
        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, String className, String fullName){
        Student existing = getStudentById(id);
        existing.setId(id);
        existing.setClassName(className);
        existing.setFullName(fullName);
        return studentRepository.save(existing);
    }

    @Transactional
    public void deleteStudent(Long id){
        studentRepository.deleteById(id);
    }

}
