package com.tutorschedule.app.service;

import com.tutorschedule.app.entity.Student;
import com.tutorschedule.app.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudent_whenConflictExists_throwsException(){
        Student conflictingStudent = new Student();
        conflictingStudent.setId(1L);
        conflictingStudent.setFullName("Arda");
        conflictingStudent.setClassName("12-MF");

        when(studentRepository.existsById(1L))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> studentService.createStudent(conflictingStudent.getId(), conflictingStudent.getClassName(), conflictingStudent.getFullName())
        );

        assertEquals("Bu numara zaten kayıtlı: 1", exception.getMessage());
    }

    @Test
    void createStudent_whenNoConflict_savesSuccessfully(){
        Student newStudent = new Student();
        newStudent.setId(1L);
        newStudent.setFullName("Arda");
        newStudent.setClassName("12-MF");

        when(studentRepository.existsById(1L))
                .thenReturn(false);

        when(studentRepository.save(newStudent))
                .thenReturn(newStudent);

        Student result = studentService.createStudent(newStudent.getId(), newStudent.getClassName(), newStudent.getFullName());

        assertEquals(newStudent, result);
    }

    @Test
    void getStudentById_whenNotExists_throwsException(){
        Student notExistingStudent = new Student();
        notExistingStudent.setId(1L);
        notExistingStudent.setFullName("Arda");
        notExistingStudent.setClassName("12-MF");

        when(studentRepository.findById(1L))
                .thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> studentService.getStudentById(notExistingStudent.getId())
        );

        assertEquals("Öğrenci bulunamadı: 1", exception.getMessage());
    }

    @Test
    void searchStudents_whenNameProvided_callsFindByFullName(){
        String name = "Arda";
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Arda");
        student.setClassName("12-MF");

        when(studentRepository.findByFullNameContainingIgnoreCase(name))
                .thenReturn(List.of(student));

        List<Student> result = studentService.searchStudents(name);

        assertEquals(List.of(student), result);
        verify(studentRepository).findByFullNameContainingIgnoreCase(name);
        verify(studentRepository, never()).findAll();
    }

    @Test
    void searchStudents_whenNameIsNull_callsFindAll(){
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Arda");
        student.setClassName("12-MF");

        when(studentRepository.findAll())
                .thenReturn(List.of(student));

        List<Student> result = studentService.searchStudents(null);

        assertEquals(List.of(student), result);
        verify(studentRepository).findAll();
        verify(studentRepository, never()).findByFullNameContainingIgnoreCase(any());
    }

    @Test
    void deleteStudent_callsRepositoryDeleteById(){
        Long id = 1L;

        studentService.deleteStudent(id);

        verify(studentRepository).deleteById(id);
    }
}
