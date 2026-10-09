package com.example.examseating.dto;

import com.example.examseating.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public class AttendanceUpdateRequest {
    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
