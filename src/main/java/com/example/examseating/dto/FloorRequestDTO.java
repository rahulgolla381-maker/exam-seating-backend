package com.example.examseating.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FloorRequestDTO {
    @NotNull(message = "Floor number is required")
    private Integer floorNumber;

    @NotBlank(message = "Floor name is required")
    private String floorName;

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }
}
