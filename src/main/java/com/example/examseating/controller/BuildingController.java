package com.example.examseating.controller;

import com.example.examseating.dto.BuildingRequestDTO;
import com.example.examseating.dto.BuildingResponseDTO;
import com.example.examseating.dto.FloorRequestDTO;
import com.example.examseating.dto.FloorResponseDTO;
import com.example.examseating.service.BuildingService;
import com.example.examseating.service.FloorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private FloorService floorService;

    @GetMapping("/summary")
    public java.util.Map<String, Object> getSummary() {
        return buildingService.getSummary();
    }

    @GetMapping
    public List<BuildingResponseDTO> getAll() {
        return buildingService.getAll();
    }

    @GetMapping("/{id}")
    public BuildingResponseDTO getById(@PathVariable Long id) {
        return buildingService.getResponseDTOById(id);
    }

    @PostMapping
    public ResponseEntity<BuildingResponseDTO> create(@Valid @RequestBody BuildingRequestDTO dto) {
        BuildingResponseDTO created = buildingService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public BuildingResponseDTO update(@PathVariable Long id, @Valid @RequestBody BuildingRequestDTO dto) {
        return buildingService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buildingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/floors")
    public List<FloorResponseDTO> getFloors(@PathVariable Long id) {
        return floorService.getByBuildingId(id);
    }

    @PostMapping("/{id}/floors")
    public ResponseEntity<FloorResponseDTO> createFloor(@PathVariable Long id, @Valid @RequestBody FloorRequestDTO dto) {
        FloorResponseDTO created = floorService.create(id, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
