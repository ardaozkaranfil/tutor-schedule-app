package com.tutorschedule.app.entity;

import jakarta.persistence.*;

/**
 * Represents a student. id is a database-generated surrogate key;
 * students are identified by name and class, not by a school number.
 */
@Entity
@Table(name = "STUDENT")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String fullName;

    public Student(){

    }

    /**
     * Builds a student from a given school number, class, and name.
     */
    public Student(Long id, String className, String fullName){
        this.setId(id);
        this.setClassName(className);
        this.setFullName(fullName);
    }

    /**
     * Builds a student without an id; the database assigns it on save.
     */
    public Student(String className, String fullName){
        this.setClassName(className);
        this.setFullName(fullName);
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getClassName() {return className;}
    public void setClassName(String className) {this.className = className;}
    public String getFullName() {return fullName;}
    public void setFullName(String fullName) {this.fullName = fullName;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Student student = (Student) o;
        return id != null && id.equals(student.getId());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
