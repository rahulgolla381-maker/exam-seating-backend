package com.example.examseating.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ExamRoomSelectionRequest {
    @NotEmpty(message = "At least one room must be selected")
    private List<Long> roomIds;

    public List<Long> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<Long> roomIds) {
        this.roomIds = roomIds;
    }
}
