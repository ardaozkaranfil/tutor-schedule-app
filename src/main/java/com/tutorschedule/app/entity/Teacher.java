package com.tutorschedule.app.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "TEACHER")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String branch;

    public Teacher(){

    }
    public Teacher(String name, String branch){
        fullName = name;
        this.branch = branch;
    }

    public Long getId() {return id;}
    public String getFullName() {return fullName;}
    public void setFullName(String fullName) {this.fullName = fullName;}
    public String getBranch() {return branch;}
    public void setBranch(String branch) {this.branch = branch;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Teacher teacher = (Teacher) o;
        return id != null && id.equals(teacher.getId());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
