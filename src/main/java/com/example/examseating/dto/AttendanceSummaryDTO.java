package com.example.examseating.dto;

public class AttendanceSummaryDTO {
    private Long examId;
    private String subjectName;
    private String subjectCode;
    private long totalStudents;
    private long presentCount;
    private long absentCount;
    private long pendingCount;
    private double attendancePercentage;

    public AttendanceSummaryDTO() {}

    public AttendanceSummaryDTO(Long examId, String subjectName, String subjectCode,
                                long totalStudents, long presentCount, long absentCount, long pendingCount) {
        this.examId = examId;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.totalStudents = totalStudents;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.pendingCount = pendingCount;
        this.attendancePercentage = totalStudents > 0 ? Math.round(((double) presentCount / totalStudents) * 1000.0) / 10.0 : 0.0;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(long absentCount) {
        this.absentCount = absentCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
