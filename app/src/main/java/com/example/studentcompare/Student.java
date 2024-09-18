package com.example.studentcompare;

public class Student {
    public String id, name, secname, group;
    public Student(){

    }

    public Student(String id, String name, String secname, String group) {
        this.id = id;
        this.name = name;
        this.secname = secname;
        this.group = group;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
