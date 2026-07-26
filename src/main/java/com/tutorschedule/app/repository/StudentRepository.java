package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Database access layer for the Student entity.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Searches for students whose name contains the given text, case-insensitively.
     */
    List<Student> findByFullNameContainingIgnoreCase(String fullName);
}
