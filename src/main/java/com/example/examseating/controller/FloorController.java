package com.example.examseating.controller;

import com.example.examseating.dto.FloorRequestDTO;
import com.example.examseating.dto.FloorResponseDTO;
import com.example.examseating.service.FloorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    @Autowired
    private FloorService floorService;

    @GetMapping
    public List<FloorResponseDTO> getFloorsByBuilding(@RequestParam Long buildingId) {
        return floorService.getByBuildingId(buildingId);
    }

    @GetMapping("/{id}")
    public FloorResponseDTO getById(@PathVariable Long id) {
        return floorService.toDTO(floorService.getById(id));
    }

    @PutMapping("/{id}")
    public FloorResponseDTO update(@PathVariable Long id, @Valid @RequestBody FloorRequestDTO dto) {
        return floorService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        floorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
