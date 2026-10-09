package com.example.examseating.dto;

import com.example.examseating.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public class BulkAttendanceRequest {
    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    private Long roomId; // Optional: if provided, updates only students in this room

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
