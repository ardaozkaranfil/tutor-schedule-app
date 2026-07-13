package com.tutorschedule.app.entity;

import jakarta.persistence.*;

import java.util.Objects;


@Entity
@Table(name = "STUDENT")
public class Student {
    @Id
    private Long id;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String fullName;

    public Student(){

    }
    public Student(Long id, String className, String fullName){
        this.setId(id);
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
