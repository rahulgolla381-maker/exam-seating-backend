package com.example.examseating.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "sections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "department_id"})
})
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "start_roll_number")
    private String startRollNumber;

    @Column(name = "end_roll_number")
    private String endRollNumber;

    @Column(name = "expected_student_count")
    private Integer expectedStudentCount;

    @Column(name = "detained_roll_numbers", columnDefinition = "TEXT")
    private String detainedRollNumbers;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getStartRollNumber() { return startRollNumber; }
    public void setStartRollNumber(String startRollNumber) { this.startRollNumber = startRollNumber; }

    public String getEndRollNumber() { return endRollNumber; }
    public void setEndRollNumber(String endRollNumber) { this.endRollNumber = endRollNumber; }

    public Integer getExpectedStudentCount() { return expectedStudentCount; }
    public void setExpectedStudentCount(Integer expectedStudentCount) { this.expectedStudentCount = expectedStudentCount; }

    public String getDetainedRollNumbers() { return detainedRollNumbers; }
    public void setDetainedRollNumbers(String detainedRollNumbers) { this.detainedRollNumbers = detainedRollNumbers; }

    @JsonProperty("detainedList")
    public List<String> getDetainedList() {
        if (detainedRollNumbers == null || detainedRollNumbers.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(detainedRollNumbers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void setDetainedList(List<String> list) {
        if (list == null || list.isEmpty()) {
            this.detainedRollNumbers = null;
        } else {
            this.detainedRollNumbers = list.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(","));
        }
    }

    @JsonProperty("detainedCount")
    public int getDetainedCount() {
        return getDetainedList().size();
    }

    @JsonProperty("eligibleStudentCount")
    public int getEligibleStudentCount() {
        int expected = expectedStudentCount != null ? expectedStudentCount : 0;
        return Math.max(0, expected - getDetainedCount());
    }

    @Transient
    @JsonProperty("actualStudentCount")
    private Long actualStudentCount;

    @Transient
    @JsonProperty("actualActiveStudentCount")
    private Long actualActiveStudentCount;

    public Long getActualStudentCount() {
        return actualStudentCount != null ? actualStudentCount : 0L;
    }

    public void setActualStudentCount(Long actualStudentCount) {
        this.actualStudentCount = actualStudentCount;
    }

    public Long getActualActiveStudentCount() {
        return actualActiveStudentCount != null ? actualActiveStudentCount : 0L;
    }

    public void setActualActiveStudentCount(Long actualActiveStudentCount) {
        this.actualActiveStudentCount = actualActiveStudentCount;
    }

    @JsonProperty("isConfigured")
    public boolean isConfigured() {
        return startRollNumber != null && !startRollNumber.trim().isEmpty() &&
               endRollNumber != null && !endRollNumber.trim().isEmpty();
    }
}
