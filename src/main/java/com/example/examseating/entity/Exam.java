package com.example.examseating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String subjectName;

    @NotBlank
    private String subjectCode;

    @NotNull
    private LocalDate examDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private Integer semester;

    @NotBlank
    private String academicYear;

    @NotBlank
    private String session; // MORNING, AFTERNOON

    private String status = "SCHEDULED";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "exam_selected_rooms",
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private List<Room> selectedRooms = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Room> getSelectedRooms() { return selectedRooms; }
    public void setSelectedRooms(List<Room> selectedRooms) { this.selectedRooms = selectedRooms; }
}
