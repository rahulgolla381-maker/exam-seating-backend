package com.example.examseating.controller;

import com.example.examseating.dto.RoomRequestDTO;
import com.example.examseating.entity.Room;
import com.example.examseating.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService service;

    @GetMapping
    public List<Room> getAll(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId) {
        if (floorId != null) {
            return service.getByFloorId(floorId);
        }
        if (buildingId != null) {
            return service.getByBuildingId(buildingId);
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Room getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<Room> create(@Valid @RequestBody Room entity) {
        Room created = service.save(entity);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/dto")
    public ResponseEntity<Room> createFromDTO(@Valid @RequestBody RoomRequestDTO dto) {
        Room created = service.createFromDTO(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Room update(@PathVariable Long id, @Valid @RequestBody Room entity) {
        entity.setId(id);
        return service.save(entity);
    }

    @PutMapping("/{id}/dto")
    public Room updateFromDTO(@PathVariable Long id, @Valid @RequestBody RoomRequestDTO dto) {
        return service.updateFromDTO(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/layout")
    public ResponseEntity<Map<String, Object>> getRoomLayout(@PathVariable Long id) {
        Room room = service.getById(id);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> layout = new HashMap<>();
        layout.put("id", room.getId());
        layout.put("roomNumber", room.getRoomNumber());
        layout.put("building", room.getBuilding());
        layout.put("floor", room.getFloor());
        layout.put("rowsCount", room.getRowsCount());
        layout.put("columnsCount", room.getColumnsCount());
        layout.put("capacity", room.getCapacity());
        layout.put("status", room.getStatus());
        return ResponseEntity.ok(layout);
    }

    @GetMapping("/{id}/conflict")
    public ResponseEntity<Map<String, Object>> checkConflict(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) Long excludeExamId) {
        String conflict = service.checkRoomConflict(id, examDate, startTime, endTime, excludeExamId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("hasConflict", conflict != null);
        resp.put("message", conflict);
        return ResponseEntity.ok(resp);
    }
}
