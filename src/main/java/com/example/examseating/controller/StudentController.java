package com.example.examseating.controller;
import com.example.examseating.entity.Student;
import com.example.examseating.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService service;

    @GetMapping
    public List<Student> getAll() { return service.getAll(); }

    @GetMapping("/{id}")
    public Student getById(@PathVariable Long id) { return service.getById(id); }

    @GetMapping("/section/{sectionId}")
    public List<Student> getBySection(@PathVariable Long sectionId) {
        return service.getBySectionId(sectionId);
    }

    @PostMapping
    public Student create(@Valid @RequestBody Student entity) { return service.save(entity); }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @Valid @RequestBody Student entity) {
        entity.setId(id);
        return service.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
