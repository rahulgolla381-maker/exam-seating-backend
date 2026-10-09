package com.example.examseating.controller;

import com.example.examseating.dto.ExamRoomSelectionRequest;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Room;
import com.example.examseating.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    @Autowired
    private ExamService service;

    @GetMapping
    public List<Exam> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public Exam getById(@PathVariable Long id) { return service.getById(id); }

    @PostMapping
    public Exam create(@Valid @RequestBody Exam entity) { return service.save(entity); }

    @PutMapping("/{id}")
    public Exam update(@PathVariable Long id, @Valid @RequestBody Exam entity) {
        entity.setId(id);
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/{id}/rooms")
    public List<Room> getSelectedRooms(@PathVariable Long id) {
        return service.getSelectedRooms(id);
    }

    @PostMapping("/{id}/rooms")
    public List<Room> setSelectedRooms(@PathVariable Long id, @Valid @RequestBody ExamRoomSelectionRequest request) {
        return service.setSelectedRooms(id, request.getRoomIds());
    }
}
