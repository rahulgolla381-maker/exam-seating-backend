package com.example.examseating.controller;

import com.example.examseating.dto.DepartmentDetailsDTO;
import com.example.examseating.entity.Department;
import com.example.examseating.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService service;

    @GetMapping
    public List<Department> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public Department getById(@PathVariable Long id) { return service.getById(id); }

    @GetMapping("/{id}/details")
    public ResponseEntity<DepartmentDetailsDTO> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDepartmentDetails(id));
    }

    @GetMapping("/{id}/dependencies")
    public ResponseEntity<DepartmentDetailsDTO> getDependencies(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDepartmentDetails(id));
    }

    @PostMapping
    public Department create(@Valid @RequestBody Department entity) { return service.save(entity); }

    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @Valid @RequestBody Department entity) {
        entity.setId(id);
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(name = "cascade", defaultValue = "false") boolean cascade) {
        if (cascade) {
            service.deleteDepartmentCascade(id);
        } else {
            service.delete(id);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/cascade")
    public ResponseEntity<Void> deleteCascade(@PathVariable Long id) {
        service.deleteDepartmentCascade(id);
        return ResponseEntity.ok().build();
    }
}
