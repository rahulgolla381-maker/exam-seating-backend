package com.example.examseating.dto;

public class AllocationResponseDTO {
    private Long allocationId;
    private Long examId;
    private Long roomId;
    private String roomNumber;
    private Long studentId;
    private String rollNumber;
    private String studentName;
    private String department;
    private String section;
    private Integer rowNumber;
    private Integer columnNumber;
    private Integer seatNumber;

    // Getters and Setters
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public Integer getColumnNumber() { return columnNumber; }
    public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
