package com.example.examseating.dto;

import java.util.ArrayList;
import java.util.List;

public class DepartmentDetailsDTO {

    private Long departmentId;
    private String departmentName;
    private String departmentCode;

    // Academic Counts
    private long sectionCount;
    private long studentCount;
    private long allocationCount;
    private long attendanceCount;

    // Section Summary
    private List<String> sectionNames = new ArrayList<>();

    public DepartmentDetailsDTO() {}

    public DepartmentDetailsDTO(Long departmentId, String departmentName, String departmentCode,
                                long sectionCount, long studentCount, long allocationCount, long attendanceCount,
                                List<String> sectionNames) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.sectionCount = sectionCount;
        this.studentCount = studentCount;
        this.allocationCount = allocationCount;
        this.attendanceCount = attendanceCount;
        this.sectionNames = sectionNames != null ? sectionNames : new ArrayList<>();
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public long getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(long sectionCount) {
        this.sectionCount = sectionCount;
    }

    public long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(long studentCount) {
        this.studentCount = studentCount;
    }

    public long getAllocationCount() {
        return allocationCount;
    }

    public void setAllocationCount(long allocationCount) {
        this.allocationCount = allocationCount;
    }

    public long getAttendanceCount() {
        return attendanceCount;
    }

    public void setAttendanceCount(long attendanceCount) {
        this.attendanceCount = attendanceCount;
    }

    public List<String> getSectionNames() {
        return sectionNames;
    }

    public void setSectionNames(List<String> sectionNames) {
        this.sectionNames = sectionNames;
    }
}
