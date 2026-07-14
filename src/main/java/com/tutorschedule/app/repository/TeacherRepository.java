package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    List<Teacher> findByFullNameContainingIgnoreCase(String fullName);
    List<Teacher> findByBranch(String branch);
    @Query("SELECT DISTINCT t.branch FROM Teacher t ORDER BY t.branch")
    List<String> findDistinctBranches();
    List<Teacher> findByFullNameContainingIgnoreCaseAndBranch(String fullName, String branch);
}
