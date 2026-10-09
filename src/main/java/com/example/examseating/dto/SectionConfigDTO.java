package com.example.examseating.dto;

import jakarta.validation.constraints.NotBlank;

public class SectionConfigDTO {
    @NotBlank(message = "Start roll number is required")
    private String startRollNumber;

    @NotBlank(message = "End roll number is required")
    private String endRollNumber;

    public SectionConfigDTO() {}

    public SectionConfigDTO(String startRollNumber, String endRollNumber) {
        this.startRollNumber = startRollNumber;
        this.endRollNumber = endRollNumber;
    }

    public String getStartRollNumber() { return startRollNumber; }
    public void setStartRollNumber(String startRollNumber) { this.startRollNumber = startRollNumber; }

    public String getEndRollNumber() { return endRollNumber; }
    public void setEndRollNumber(String endRollNumber) { this.endRollNumber = endRollNumber; }
}
