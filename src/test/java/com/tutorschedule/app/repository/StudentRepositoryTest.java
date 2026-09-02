package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findByFullNameContainingIgnoreCase_returnsMatch_whenCaseDiffers() {
        studentRepository.save(new Student("5A", "Ahmet Yilmaz"));

        List<Student> result = studentRepository.findByFullNameContainingIgnoreCase("ahmet");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFullName()).isEqualTo("Ahmet Yilmaz");
    }

    @Test
    void findByFullNameContainingIgnoreCase_returnsEmpty_whenNoMatch() {
        studentRepository.save(new Student("5A", "Ahmet Yilmaz"));

        List<Student> result = studentRepository.findByFullNameContainingIgnoreCase("Mehmet");

        assertThat(result).isEmpty();
    }

    @Test
    void findByFullNameContainingIgnoreCase_returnsPartialMatch_whenSubstring() {
        studentRepository.save(new Student("5A", "Ahmet Yilmaz"));
        studentRepository.save(new Student("5B", "Ahmet Kaya"));
        studentRepository.save(new Student("5A", "Zeynep Demir"));

        List<Student> result = studentRepository.findByFullNameContainingIgnoreCase("Ahmet");

        assertThat(result).hasSize(2)
                .extracting(Student::getFullName)
                .containsExactlyInAnyOrder("Ahmet Yilmaz", "Ahmet Kaya");
    }
}