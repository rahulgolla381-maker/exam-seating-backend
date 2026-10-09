package com.example.examseating.controller;

import com.example.examseating.dto.BatchSectionCreateDTO;
import com.example.examseating.dto.DetainedRollRequestDTO;
import com.example.examseating.dto.SectionConfigDTO;
import com.example.examseating.dto.SectionDetailsDTO;
import com.example.examseating.entity.Section;
import com.example.examseating.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionController {

    @Autowired
    private SectionService service;

    @GetMapping
    public List<Section> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Section getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<SectionDetailsDTO> getSectionDetails(@PathVariable Long id) {
        SectionDetailsDTO dto = service.getSectionDetails(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/department/{departmentId}")
    public List<Section> getByDepartment(@PathVariable Long departmentId) {
        return service.getByDepartmentId(departmentId);
    }

    @PostMapping
    public Section create(@Valid @RequestBody Section entity) {
        return service.save(entity);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Section>> createBatch(@Valid @RequestBody BatchSectionCreateDTO dto) {
        List<Section> created = service.createBatchSections(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Section update(@PathVariable Long id, @Valid @RequestBody Section entity) {
        entity.setId(id);
        return service.save(entity);
    }

    @PutMapping("/{id}/config")
    public ResponseEntity<Section> configurePattern(@PathVariable Long id, @Valid @RequestBody SectionConfigDTO dto) {
        Section updated = service.configurePattern(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/detained")
    public ResponseEntity<Section> addDetained(@PathVariable Long id, @Valid @RequestBody DetainedRollRequestDTO dto) {
        Section updated = service.addDetainedRollNumber(id, dto.getRollNumber());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/detained/{rollNumber}")
    public ResponseEntity<Section> removeDetained(@PathVariable Long id, @PathVariable String rollNumber) {
        Section updated = service.removeDetainedRollNumber(id, rollNumber);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/detained")
    public ResponseEntity<List<String>> getDetainedList(@PathVariable Long id) {
        List<String> detained = service.getDetainedRollNumbers(id);
        return ResponseEntity.ok(detained);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Section> syncRoster(@PathVariable Long id) {
        Section synced = service.syncSectionRosterById(id);
        return ResponseEntity.ok(synced);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(name = "cascade", defaultValue = "false") boolean cascade) {
        if (cascade) {
            service.deleteSectionCascade(id);
        } else {
            service.delete(id);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/cascade")
    public ResponseEntity<Void> deleteCascade(@PathVariable Long id) {
        service.deleteSectionCascade(id);
        return ResponseEntity.ok().build();
    }
}
