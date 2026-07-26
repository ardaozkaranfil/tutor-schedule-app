package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Database access layer for the Teacher entity.
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Searches for teachers whose name contains the given text, case-insensitively.
     */
    List<Teacher> findByFullNameContainingIgnoreCase(String fullName);

    /**
     * Returns teachers belonging to a given branch.
     */
    List<Teacher> findByBranch(String branch);

    /**
     * Returns every distinct branch name currently in use, alphabetically —
     * feeds the branch dropdown on the filter screen.
     */
    @Query("SELECT DISTINCT t.branch FROM Teacher t ORDER BY t.branch")
    List<String> findDistinctBranches();

    /**
     * Filters by name and branch together.
     */
    List<Teacher> findByFullNameContainingIgnoreCaseAndBranch(String fullName, String branch);
}
