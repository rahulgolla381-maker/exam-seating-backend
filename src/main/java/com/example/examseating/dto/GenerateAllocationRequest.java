package com.example.examseating.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class GenerateAllocationRequest {
    @NotNull
    private Long examId;
    
    @NotEmpty
    private List<Long> studentIds;
    
    @NotEmpty
    private List<Long> roomIds;

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public List<Long> getStudentIds() { return studentIds; }
    public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }
    public List<Long> getRoomIds() { return roomIds; }
    public void setRoomIds(List<Long> roomIds) { this.roomIds = roomIds; }
}
