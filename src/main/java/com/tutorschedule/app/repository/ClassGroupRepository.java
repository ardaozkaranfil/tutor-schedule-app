package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, String> {
    List<ClassGroup> findByNameContainingIgnoreCase(String name);
}
