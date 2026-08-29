package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.ClassGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClassGroupRepositoryTest {

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Test
    void save_persistsWithGivenStringIdAsNaturalKey() {
        classGroupRepository.save(new ClassGroup("9-A"));

        Optional<ClassGroup> result = classGroupRepository.findById("9-A");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("9-A");
    }

    @Test
    void findByNameContainingIgnoreCase_returnsMatch_whenCaseDiffers() {
        classGroupRepository.save(new ClassGroup("9-A"));

        List<ClassGroup> result = classGroupRepository.findByNameContainingIgnoreCase("9-a");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("9-A");
    }

    @Test
    void findByNameContainingIgnoreCase_returnsPartialMatch_whenSubstring() {
        classGroupRepository.save(new ClassGroup("9-A"));
        classGroupRepository.save(new ClassGroup("9-B"));
        classGroupRepository.save(new ClassGroup("10-A"));

        List<ClassGroup> result = classGroupRepository.findByNameContainingIgnoreCase("9-");

        assertThat(result).hasSize(2)
                .extracting(ClassGroup::getName)
                .containsExactlyInAnyOrder("9-A", "9-B");
    }
}