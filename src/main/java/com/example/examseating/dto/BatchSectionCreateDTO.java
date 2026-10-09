package com.example.examseating.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BatchSectionCreateDTO {
    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotEmpty(message = "Section names list cannot be empty")
    private List<String> sectionNames;

    public BatchSectionCreateDTO() {}

    public BatchSectionCreateDTO(Long departmentId, List<String> sectionNames) {
        this.departmentId = departmentId;
        this.sectionNames = sectionNames;
    }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public List<String> getSectionNames() { return sectionNames; }
    public void setSectionNames(List<String> sectionNames) { this.sectionNames = sectionNames; }
}
