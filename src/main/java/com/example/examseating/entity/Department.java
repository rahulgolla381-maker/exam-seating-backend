package com.example.examseating.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String name;

    @NotBlank
    @Column(unique = true)
    private String code;

    @Transient
    private Long sectionCount;

    @Transient
    private Long studentCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getSectionCount() { return sectionCount != null ? sectionCount : 0L; }
    public void setSectionCount(Long sectionCount) { this.sectionCount = sectionCount; }

    public Long getStudentCount() { return studentCount != null ? studentCount : 0L; }
    public void setStudentCount(Long studentCount) { this.studentCount = studentCount; }
}
