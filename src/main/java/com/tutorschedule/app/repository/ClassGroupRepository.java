package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Database access layer for the ClassGroup entity.
 */
@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, String> {

    /**
     * Searches for class names containing the given text, case-insensitively.
     */
    List<ClassGroup> findByNameContainingIgnoreCase(String name);
}
