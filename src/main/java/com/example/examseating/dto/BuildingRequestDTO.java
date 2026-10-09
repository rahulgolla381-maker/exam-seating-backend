package com.example.examseating.dto;

import jakarta.validation.constraints.NotBlank;

public class BuildingRequestDTO {

    @NotBlank(message = "Building code is required")
    private String code;

    @NotBlank(message = "Building name is required")
    private String name;

    private String description;

    private String status = "ACTIVE";

    private Integer floorsCount; // If provided, auto-generates floors 0 to N-1

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFloorsCount() {
        return floorsCount;
    }

    public void setFloorsCount(Integer floorsCount) {
        this.floorsCount = floorsCount;
    }
}
