package com.example.examseating.dto;

import jakarta.validation.constraints.NotBlank;

public class DetainedRollRequestDTO {
    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    public DetainedRollRequestDTO() {}

    public DetainedRollRequestDTO(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
}
