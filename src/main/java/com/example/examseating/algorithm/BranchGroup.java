package com.example.examseating.algorithm;
import com.example.examseating.entity.Student;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class BranchGroup implements Comparable<BranchGroup> {
    private String groupName;
    private Stack<Student> students;

    public BranchGroup(String groupName) {
        this.groupName = groupName;
        this.students = new Stack<>();
    }

    public void addStudent(Student s) {
        this.students.push(s);
    }

    public Student popStudent() {
        return this.students.pop();
    }
    
    public void pushStudent(Student s) {
        this.students.push(s);
    }

    public int getRemaining() {
        return this.students.size();
    }

    public String getGroupName() {
        return groupName;
    }

    public List<Student> getStudents() {
        return new java.util.ArrayList<>(this.students);
    }

    @Override
    public int compareTo(BranchGroup other) {
        // Max-heap: largest remaining first
        return Integer.compare(other.getRemaining(), this.getRemaining());
    }
}
