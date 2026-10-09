package com.example.examseating.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String rollNumber;

    private String fullName;

    private String email;

    private String phoneNumber;
    
    private Integer semester = 1;
    
    private Integer year = 1;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "section_id")
    private Section section;

    private String status = "ACTIVE";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
