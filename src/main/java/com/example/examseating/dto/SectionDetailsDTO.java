package com.example.examseating.dto;

import java.util.ArrayList;
import java.util.List;

public class SectionDetailsDTO {

    // Section Information
    private Long sectionId;
    private String sectionName;
    private Long departmentId;
    private String departmentName;
    private String departmentCode;

    // Roll Configuration
    private String startRollNumber;
    private String endRollNumber;
    private Integer expectedStudentCount;
    private String detainedRollNumbers;
    private int detainedCount;
    private int eligibleStudentCount;
    private boolean isConfigured;

    // Actual Database Data
    private long actualStudentCount;
    private long actualActiveStudentCount;
    private long actualInactiveStudentCount;
    private long actualDetainedStudentCount;
    private String consistencyStatus;
    private String consistencyCode; // "MATCH", "MISMATCH", "UNCONFIGURED"

    // Database Relationships / Dependencies
    private long studentCount;
    private long allocationCount;
    private long attendanceCount;
    private boolean canDelete;
    private String deleteBlockedReason;

    // Expandable Detailed Lists
    private List<StudentItemDTO> students = new ArrayList<>();
    private List<AllocationItemDTO> seatAllocations = new ArrayList<>();
    private List<AttendanceItemDTO> attendances = new ArrayList<>();

    public SectionDetailsDTO() {}

    // Getters and Setters
    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getStartRollNumber() { return startRollNumber; }
    public void setStartRollNumber(String startRollNumber) { this.startRollNumber = startRollNumber; }

    public String getEndRollNumber() { return endRollNumber; }
    public void setEndRollNumber(String endRollNumber) { this.endRollNumber = endRollNumber; }

    public Integer getExpectedStudentCount() { return expectedStudentCount; }
    public void setExpectedStudentCount(Integer expectedStudentCount) { this.expectedStudentCount = expectedStudentCount; }

    public String getDetainedRollNumbers() { return detainedRollNumbers; }
    public void setDetainedRollNumbers(String detainedRollNumbers) { this.detainedRollNumbers = detainedRollNumbers; }

    public int getDetainedCount() { return detainedCount; }
    public void setDetainedCount(int detainedCount) { this.detainedCount = detainedCount; }

    public int getEligibleStudentCount() { return eligibleStudentCount; }
    public void setEligibleStudentCount(int eligibleStudentCount) { this.eligibleStudentCount = eligibleStudentCount; }

    public boolean isConfigured() { return isConfigured; }
    public void setConfigured(boolean configured) { isConfigured = configured; }

    public long getActualStudentCount() { return actualStudentCount; }
    public void setActualStudentCount(long actualStudentCount) { this.actualStudentCount = actualStudentCount; }

    public long getActualActiveStudentCount() { return actualActiveStudentCount; }
    public void setActualActiveStudentCount(long actualActiveStudentCount) { this.actualActiveStudentCount = actualActiveStudentCount; }

    public long getActualInactiveStudentCount() { return actualInactiveStudentCount; }
    public void setActualInactiveStudentCount(long actualInactiveStudentCount) { this.actualInactiveStudentCount = actualInactiveStudentCount; }

    public long getActualDetainedStudentCount() { return actualDetainedStudentCount; }
    public void setActualDetainedStudentCount(long actualDetainedStudentCount) { this.actualDetainedStudentCount = actualDetainedStudentCount; }

    public String getConsistencyStatus() { return consistencyStatus; }
    public void setConsistencyStatus(String consistencyStatus) { this.consistencyStatus = consistencyStatus; }

    public String getConsistencyCode() { return consistencyCode; }
    public void setConsistencyCode(String consistencyCode) { this.consistencyCode = consistencyCode; }

    public long getStudentCount() { return studentCount; }
    public void setStudentCount(long studentCount) { this.studentCount = studentCount; }

    public long getAllocationCount() { return allocationCount; }
    public void setAllocationCount(long allocationCount) { this.allocationCount = allocationCount; }

    public long getAttendanceCount() { return attendanceCount; }
    public void setAttendanceCount(long attendanceCount) { this.attendanceCount = attendanceCount; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public String getDeleteBlockedReason() { return deleteBlockedReason; }
    public void setDeleteBlockedReason(String deleteBlockedReason) { this.deleteBlockedReason = deleteBlockedReason; }

    public List<StudentItemDTO> getStudents() { return students; }
    public void setStudents(List<StudentItemDTO> students) { this.students = students; }

    public List<AllocationItemDTO> getSeatAllocations() { return seatAllocations; }
    public void setSeatAllocations(List<AllocationItemDTO> seatAllocations) { this.seatAllocations = seatAllocations; }

    public List<AttendanceItemDTO> getAttendances() { return attendances; }
    public void setAttendances(List<AttendanceItemDTO> attendances) { this.attendances = attendances; }

    // Nested DTOs
    public static class StudentItemDTO {
        private Long id;
        private String rollNumber;
        private String fullName;
        private String email;
        private String phoneNumber;
        private Integer semester;
        private Integer year;
        private String departmentName;
        private String departmentCode;
        private String sectionName;
        private String status;

        public StudentItemDTO() {}

        public StudentItemDTO(Long id, String rollNumber, String fullName, String email, String phoneNumber,
                              Integer semester, Integer year, String departmentName, String departmentCode,
                              String sectionName, String status) {
            this.id = id;
            this.rollNumber = rollNumber;
            this.fullName = fullName;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.semester = semester;
            this.year = year;
            this.departmentName = departmentName;
            this.departmentCode = departmentCode;
            this.sectionName = sectionName;
            this.status = status;
        }

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
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AllocationItemDTO {
        private Long id;
        private Long examId;
        private String subjectCode;
        private String subjectName;
        private String roomNumber;
        private String building;
        private Integer seatNumber;
        private Integer rowNumber;
        private Integer columnNumber;
        private Long studentId;
        private String studentRoll;
        private String studentName;

        public AllocationItemDTO() {}

        public AllocationItemDTO(Long id, Long examId, String subjectCode, String subjectName,
                                 String roomNumber, String building, Integer seatNumber,
                                 Integer rowNumber, Integer columnNumber, Long studentId,
                                 String studentRoll, String studentName) {
            this.id = id;
            this.examId = examId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.roomNumber = roomNumber;
            this.building = building;
            this.seatNumber = seatNumber;
            this.rowNumber = rowNumber;
            this.columnNumber = columnNumber;
            this.studentId = studentId;
            this.studentRoll = studentRoll;
            this.studentName = studentName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getExamId() { return examId; }
        public void setExamId(Long examId) { this.examId = examId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRowNumber() { return rowNumber; }
        public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
        public Integer getColumnNumber() { return columnNumber; }
        public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getStudentRoll() { return studentRoll; }
        public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
    }

    public static class AttendanceItemDTO {
        private Long id;
        private Long examId;
        private String subjectCode;
        private String roomNumber;
        private Integer seatNumber;
        private String status;
        private String markedAt;
        private Long studentId;
        private String studentRoll;
        private String studentName;

        public AttendanceItemDTO() {}

        public AttendanceItemDTO(Long id, Long examId, String subjectCode, String roomNumber,
                                 Integer seatNumber, String status, String markedAt,
                                 Long studentId, String studentRoll, String studentName) {
            this.id = id;
            this.examId = examId;
            this.subjectCode = subjectCode;
            this.roomNumber = roomNumber;
            this.seatNumber = seatNumber;
            this.status = status;
            this.markedAt = markedAt;
            this.studentId = studentId;
            this.studentRoll = studentRoll;
            this.studentName = studentName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getExamId() { return examId; }
        public void setExamId(Long examId) { this.examId = examId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMarkedAt() { return markedAt; }
        public void setMarkedAt(String markedAt) { this.markedAt = markedAt; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getStudentRoll() { return studentRoll; }
        public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
    }
}
