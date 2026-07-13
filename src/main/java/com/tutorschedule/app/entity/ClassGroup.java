package com.tutorschedule.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CLASS_GROUP")
public class ClassGroup {
    @Id
    private String name;

    public ClassGroup(){

    }
    public ClassGroup(String name){
        this.name = name;
    }

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClassGroup classGroup = (ClassGroup) o;
        return name != null && name.equals(classGroup.getName());
    }

    @Override
    public int hashCode(){
        return getClass().hashCode();
    }
}
