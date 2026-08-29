package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Teacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TeacherRepositoryTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void findByFullNameContainingIgnoreCase_returnsMatch_whenCaseDiffers() {
        teacherRepository.save(new Teacher("Ahmet Yilmaz", "Math"));

        List<Teacher> result = teacherRepository.findByFullNameContainingIgnoreCase("ahmet");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Ahmet Yilmaz");
    }

    @Test
    void findByBranch_returnsOnlyThatBranch() {
        teacherRepository.save(new Teacher("Ahmet Yilmaz", "Math"));
        teacherRepository.save(new Teacher("Zeynep Demir", "Physics"));
        teacherRepository.save(new Teacher("Mehmet Kaya", "Math"));

        List<Teacher> result = teacherRepository.findByBranch("Math");

        assertThat(result).hasSize(2)
                .extracting(Teacher::getFullName)
                .containsExactlyInAnyOrder("Ahmet Yilmaz", "Mehmet Kaya");
    }

    @Test
    void findDistinctBranches_returnsUniqueSortedBranches() {
        teacherRepository.save(new Teacher("Ahmet Yilmaz", "Physics"));
        teacherRepository.save(new Teacher("Zeynep Demir", "Math"));
        teacherRepository.save(new Teacher("Mehmet Kaya", "Physics"));

        List<String> result = teacherRepository.findDistinctBranches();

        assertThat(result).containsExactly("Math", "Physics");
    }

    @Test
    void findByFullNameContainingIgnoreCaseAndBranch_appliesBothFilters() {
        teacherRepository.save(new Teacher("Ahmet Yilmaz", "Math"));
        teacherRepository.save(new Teacher("Ahmet Kaya", "Physics"));
        teacherRepository.save(new Teacher("Zeynep Ahmet", "Math"));

        List<Teacher> result = teacherRepository.findByFullNameContainingIgnoreCaseAndBranch("ahmet", "Math");

        assertThat(result).hasSize(2)
                .extracting(Teacher::getFullName)
                .containsExactlyInAnyOrder("Ahmet Yilmaz", "Zeynep Ahmet");
    }
}